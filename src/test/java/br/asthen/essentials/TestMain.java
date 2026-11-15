package br.asthen.essentials;

import br.asthen.essentials.core.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** JDK-only tests for the pure core (no Paper needed). */
public final class TestMain {
    static int pass, fail;

    static void check(String name, boolean ok) {
        if (ok) pass++;
        else {
            fail++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] a) throws Exception {
        Path dir = Files.createTempDirectory("ae-test");
        money(dir);
        economy(dir);
        places(dir);
        tpa();
        cooldowns();
        punish(dir);
        System.out.println(pass + " ok, " + fail + " falhas");
        if (fail > 0) System.exit(1);
    }

    static void money(Path d) {
        check("parse 10", Money.parse("10") == 1000);
        check("parse 10,5", Money.parse("10,5") == 1050);
        check("parse 10.05", Money.parse("10.05") == 1005);
        check("parse invalid", Money.parse("abc") == -1 && Money.parse("-1") == -1 && Money.parse("1,234") == -1);
        check("parse too big", Money.parse("9999999999") == -1);
        check("format", Money.format(123456789, "$").equals("$1.234.567,89"));
        check("format small", Money.format(5, "$").equals("$0,05"));
    }

    static void economy(Path d) {
        Path f = d.resolve("eco.json");
        Economy e = new Economy(f, 10000);
        UUID x = UUID.randomUUID(), y = UUID.randomUUID();
        e.touch(x, "Ana");
        e.touch(y, "Bia");
        check("start balance", e.balance(x) == 10000);
        check("pay ok", e.transfer(x, y, 2500) == Economy.Result.OK && e.balance(x) == 7500 && e.balance(y) == 12500);
        check("pay no funds changes nothing", e.transfer(x, y, 99999) == Economy.Result.NO_FUNDS && e.balance(x) == 7500 && e.balance(y) == 12500);
        check("pay self", e.transfer(x, x, 100) == Economy.Result.SAME_ACCOUNT);
        check("pay zero/negative", e.transfer(x, y, 0) == Economy.Result.INVALID_AMOUNT && e.transfer(x, y, -5) == Economy.Result.INVALID_AMOUNT);
        check("overflow", e.set(y, Money.MAX) == Economy.Result.OK && e.transfer(x, y, 100) == Economy.Result.OVERFLOW && e.balance(x) == 7500);
        check("take too much", e.take(x, 8000) == Economy.Result.NO_FUNDS && e.balance(x) == 7500);
        check("give/take", e.give(x, 500) == Economy.Result.OK && e.take(x, 8000) == Economy.Result.OK && e.balance(x) == 0);
        check("top order", e.top(5).get(0).id().equals(y));
        check("find by name", y.equals(e.findByName("bia")));
        Economy again = new Economy(f, 10000);
        check("persisted", again.balance(y) == Money.MAX && again.balance(x) == 0 && again.nameOf(x).equals("Ana"));
        // corrupt file is preserved, not crashing
        try {
            Path bad = d.resolve("bad.json");
            Files.writeString(bad, "{not json");
            Economy c = new Economy(bad, 100);
            check("corrupt file tolerated", c.balance(x) == 100 && Files.exists(d.resolve("bad.json.bad")));
        } catch (Exception ex) {
            check("corrupt file tolerated", false);
        }
    }

    static void places(Path d) {
        Path f = d.resolve("places.json");
        Places p = new Places(f);
        UUID u = UUID.randomUUID();
        Loc l = new Loc("world", 1.5, 64, -3, 90f, 10f);
        check("sethome", p.setHome(u, "Casa", l, 2) == Places.Result.OK);
        check("home case-insens", p.home(u, "casa").equals(l));
        check("sethome same name overwrites at limit", p.setHome(u, "casa", l, 1) == Places.Result.OK);
        check("home limit", p.setHome(u, "outra", l, 1) == Places.Result.LIMIT);
        check("invalid names", p.setHome(u, "../x", l, 5) == Places.Result.INVALID_NAME
                && p.setWarp("a b", l) == Places.Result.INVALID_NAME);
        check("warp", p.setWarp("Loja", l) == Places.Result.OK && p.warp("loja") != null);
        p.setSpawn(l);
        Places again = new Places(f);
        check("persisted", again.home(u, "casa").equals(l) && again.warp("LOJA").equals(l) && again.spawn().equals(l));
        check("delhome", again.delHome(u, "casa") == Places.Result.OK && again.delHome(u, "casa") == Places.Result.NOT_FOUND);
        check("delwarp", again.delWarp("loja") == Places.Result.OK && again.warpNames().isEmpty());
    }

    static void tpa() {
        AtomicLong now = new AtomicLong(0);
        TpaManager t = new TpaManager(60, now::get);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        t.create(a, b);
        check("take live", t.take(b) != null && t.take(b) == null);
        t.create(a, b);
        now.set(61_000);
        check("expired", t.take(b) == null);
        t.create(a, b);
        t.create(c, b);
        check("newer replaces older", t.take(b).from().equals(c));
        t.create(a, b);
        t.forget(a);
        check("forget on quit", t.take(b) == null);
    }

    static void cooldowns() {
        AtomicLong now = new AtomicLong(0);
        Cooldowns c = new Cooldowns(now::get);
        c.start("x", 10);
        check("cooldown active", c.remaining("x") == 10);
        now.set(9_500);
        check("cooldown rounds up", c.remaining("x") == 1);
        now.set(10_001);
        check("cooldown over", c.remaining("x") == 0);
    }

    static void punish(Path d) {
        AtomicLong now = new AtomicLong(1_000);
        Path f = d.resolve("pun.json");
        Punishments p = new Punishments(f, now::get);
        UUID u = UUID.randomUUID();
        p.add(Punishments.Kind.BAN, u, "Zed", "grief", "Admin", 60_000);
        check("ban active", p.active(Punishments.Kind.BAN, u) != null);
        check("kinds separate", p.active(Punishments.Kind.MUTE, u) == null);
        now.set(62_000);
        check("ban expires", p.active(Punishments.Kind.BAN, u) == null);
        p.add(Punishments.Kind.MUTE, u, "Zed", "", "Admin", 0);
        now.set(999_999_999);
        check("perm mute stays", p.active(Punishments.Kind.MUTE, u).permanent());
        check("default reason", p.active(Punishments.Kind.MUTE, u).reason().equals("Sem motivo"));
        Punishments again = new Punishments(f, now::get);
        check("persisted", again.active(Punishments.Kind.MUTE, u) != null && u.equals(again.findByName(Punishments.Kind.MUTE, "zed")));
        check("remove", again.remove(Punishments.Kind.MUTE, u) && !again.remove(Punishments.Kind.MUTE, u));
        check("duration parse", Punishments.parseDuration("30m") == 1_800_000 && Punishments.parseDuration("1w") == 604_800_000L);
        check("duration invalid", Punishments.parseDuration("0m") == -1 && Punishments.parseDuration("5x") == -1
                && Punishments.parseDuration("999d") == -1 && Punishments.parseDuration("m") == -1);
        check("duration format", Punishments.formatDuration(90_000).equals("1min") && Punishments.formatDuration(3_700_000).equals("1h 1min"));
    }
}
