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
        places(dir);
        legacyWarps(dir);
        settings(dir);
        tpa();
        pickups(dir);
        cooldowns();
        punish(dir);
        System.out.println(pass + " ok, " + fail + " falhas");
        if (fail > 0) System.exit(1);
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
                && p.setHome(u, "a b", l, 5) == Places.Result.INVALID_NAME
                && p.setHome(u, "", l, 5) == Places.Result.INVALID_NAME
                && p.setHome(u, "x".repeat(25), l, 5) == Places.Result.INVALID_NAME);
        check("accented names", Names.validPlace("Mineração") && Names.validPlace("casa_do-zé") && !Names.validPlace("casa!"));
        Places again = new Places(f);
        check("persisted", again.home(u, "casa").equals(l));
        check("accented home persisted", again.setHome(u, "Cabeça", l, 5) == Places.Result.OK
                && new Places(f).home(u, "cabeça") != null);
        check("delhome", again.delHome(u, "casa") == Places.Result.OK && again.delHome(u, "casa") == Places.Result.NOT_FOUND);
        check("homeNames", again.homeNames(u).equals(java.util.List.of("cabeça")));
    }

    static void settings(Path d) throws Exception {
        Path f = d.resolve("settings.json");
        UUID u = UUID.randomUUID();
        PlayerSettings s = new PlayerSettings(f);
        check("tpa window on by default", s.tpaWindow(u));
        s.setTpaWindow(u, false);
        check("tpa window off", !s.tpaWindow(u) && s.tpaWindow(UUID.randomUUID()));
        check("tpa window off persisted", !new PlayerSettings(f).tpaWindow(u));
        s.setTpaWindow(u, true);
        check("tpa window back on persisted", new PlayerSettings(f).tpaWindow(u));
        Files.writeString(f, "{\"tpaWindowOff\":{\"nao-e-uuid\":true}}");
        check("bad key ignored", new PlayerSettings(f).tpaWindow(u));
    }

    static void legacyWarps(Path d) throws Exception {
        Path f = d.resolve("old-places.json");
        UUID u = UUID.randomUUID();
        Files.writeString(f, "{\"homes\":{\"" + u + "\":{\"casa\":{\"world\":\"w\",\"x\":1,\"y\":2,\"z\":3,\"yaw\":0,\"pitch\":0}}},"
                + "\"warps\":{\"loja\":{\"world\":\"w\",\"x\":9,\"y\":9,\"z\":9,\"yaw\":0,\"pitch\":0}}}");
        Places p = new Places(f);
        check("legacy file: homes still load", p.home(u, "casa") != null);
        p.setHome(u, "outra", p.home(u, "casa"), 5);
        check("legacy file: warps dropped on save", !Files.readString(f).contains("warps"));
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
        check("list active", again.list(Punishments.Kind.MUTE).size() == 1 && again.list(Punishments.Kind.BAN).isEmpty());
        UUID u2 = UUID.randomUUID();
        again.add(Punishments.Kind.BAN, u2, "Temp", "x", "Admin", 1_000);
        check("list keeps unexpired ban", again.list(Punishments.Kind.BAN).size() == 1);
        now.set(now.get() + 5_000);
        check("list drops expired ban", again.list(Punishments.Kind.BAN).isEmpty());
        check("remove", again.remove(Punishments.Kind.MUTE, u) && !again.remove(Punishments.Kind.MUTE, u));
        check("duration parse", Punishments.parseDuration("30m") == 1_800_000 && Punishments.parseDuration("1w") == 604_800_000L);
        check("duration invalid", Punishments.parseDuration("0m") == -1 && Punishments.parseDuration("5x") == -1
                && Punishments.parseDuration("999d") == -1 && Punishments.parseDuration("m") == -1);
        check("duration format", Punishments.formatDuration(90_000).equals("1min") && Punishments.formatDuration(3_700_000).equals("1h 1min"));
    }

    static void pickups(Path d) {
        Path f = d.resolve("pickups.json");
        Pickups p = new Pickups(f);
        check("pickup empty", !p.has("survival", 1, 2, 3));
        check("pickup add", p.add("lobby", 10, 64, -5) && !p.add("lobby", 10, 64, -5));
        check("pickup has", p.has("lobby", 10, 64, -5) && !p.has("lobby", 10, 65, -5) && !p.has("other", 10, 64, -5));
        check("pickup world case-insens", p.has("LOBBY", 10, 64, -5));
        Pickups again = new Pickups(f);
        check("pickup persisted", again.has("lobby", 10, 64, -5) && again.count() == 1);
        check("pickup remove", again.remove("lobby", 10, 64, -5) && !again.remove("lobby", 10, 64, -5) && again.count() == 0);
    }
}
