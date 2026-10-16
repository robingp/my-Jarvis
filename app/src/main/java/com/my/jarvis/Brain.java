package com.my.jarvis;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Brain {

    public static class Result {
        public String reply;
        public String action;
        public String arg;
        Result(String r) { reply = r; }
        Result(String r, String a, String g) { reply = r; action = a; arg = g; }
    }

    private final SharedPreferences prefs;
    private final SharedPreferences memory;
    private String pendingTeachKey = null;
    private final Random rnd = new Random();

    public Brain(Context c) {
        prefs = c.getSharedPreferences("jarvis_taught", Context.MODE_PRIVATE);
        memory = c.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE);
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
    }
    private static boolean has(String s, String... words) {
        for (String w : words) if ((" " + s + " ").contains(" " + w + " ")) return true;
        return false;
    }
    private String pick(String... a) { return a[rnd.nextInt(a.length)]; }

    public String getName() { return memory.getString("userName", "sir"); }

    public Result handle(String raw) {
        String s = norm(raw);
        String name = getName();

        if (pendingTeachKey != null) {
            String key = pendingTeachKey;
            pendingTeachKey = null;
            if (has(s, "skip", "no", "nothing", "leave", "cancel")) return new Result("As you wish, " + name + ".");
            prefs.edit().putString(key, raw.trim()).apply();
            return new Result("Noted. I will remember that, " + name + ".");
        }

        Matcher tm = Pattern.compile("when i say (.+?),? you (?:say|reply) (.+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (tm.find()) {
            prefs.edit().putString(norm(tm.group(1)), tm.group(2).trim()).apply();
            return new Result("Understood. Command learned, " + name + ".");
        }

        String taught = prefs.getString(s, null);
        if (taught != null) return new Result(taught);

        if (has(s, "flashlight", "torch", "light")) {
            if (has(s, "off", "band", "close")) return new Result("Flashlight off.", "FLASHLIGHT_OFF", null);
            return new Result("Flashlight on, " + name + ".", "FLASHLIGHT_ON", null);
        }
        Matcher om = Pattern.compile("open (.+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (om.find()) {
            String app = om.group(1).trim();
            return new Result("Opening " + app + ".", "OPEN", app);
        }
        if (has(s, "scroll down", "niche")) return new Result("Scrolling.", "SCROLL_DOWN", null);
        if (has(s, "scroll up", "upar")) return new Result("Scrolling up.", "SCROLL_UP", null);
        if (has(s, "go back", "back", "wapas")) return new Result("Going back.", "BACK", null);
        if (has(s, "home", "ghar")) return new Result("Home.", "HOME", null);
        if (has(s, "read screen", "what is on screen", "read this")) return new Result("", "READ_SCREEN", null);

        Matcher nm = Pattern.compile("my name is ([\\p{L} ]{2,30})", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (nm.find()) {
            memory.edit().putString("userName", nm.group(1).trim()).apply();
            return new Result("A pleasure, " + nm.group(1).trim() + ".");
        }
        Matcher fm = Pattern.compile("\\bmy ([\\p{L} ]{2,25}?) (?:is|are) (.+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (fm.find() && !raw.toLowerCase().contains("what")) {
            String key = "fact_" + norm(fm.group(1)).replace(" ", "_");
            memory.edit().putString(key, "Your " + fm.group(1) + " is " + fm.group(2).replaceAll("[.!]$", "")).apply();
            return new Result("I will remember that, " + name + ".");
        }
        Matcher rm = Pattern.compile("^remember (?:that )?(.+)", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (rm.find()) {
            memory.edit().putString("note_" + System.currentTimeMillis(), rm.group(1)).apply();
            return new Result("Stored, " + name + ".");
        }
        Matcher qm = Pattern.compile("what(?:'s| is) my ([\\p{L} ]{2,25})", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (qm.find()) {
            String val = memory.getString("fact_" + norm(qm.group(1)).replace(" ", "_"), null);
            return new Result(val != null ? val + "." : "You have not told me your " + qm.group(1) + " yet, " + name + ".");
        }

        if (has(s, "time", "samay")) return new Result("It is " +
                new java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(new java.util.Date()) + ".");
        if (has(s, "date", "today", "aaj")) return new Result("Today is " +
                new java.text.SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new java.util.Date()) + ".");

        if (has(s, "hi", "hello", "hey", "jarvis", "namaste"))
            return new Result(pick("At your service, " + name + ".", "Yes, " + name + "?", "I am here, " + name + "."));
        if (has(s, "how are you", "kaise ho"))
            return new Result(pick("All systems operational, " + name + ".", "Functioning perfectly. And you, " + name + "?"));
        if (has(s, "thank", "thanks", "shukriya"))
            return new Result(pick("Always, " + name + ".", "My pleasure, " + name + "."));
        if (has(s, "who are you", "your name"))
            return new Result("I am JARVIS, your personal assistant, running entirely on your phone.");
        if (has(s, "what can you do", "help", "features"))
            return new Result("I can open apps, control the flashlight, scroll and go back inside apps, remember facts about you, and learn new replies when you teach me, " + name + ".");
        if (has(s, "joke", "funny"))
            return new Result(pick("Why did the phone need glasses? It lost its contacts.", "I would tell you a UDP joke, but you might not get it."));
        if (has(s, "bye", "good night", "goodnight"))
            return new Result(pick("Goodbye, " + name + ".", "Rest well, " + name + "."));

        pendingTeachKey = s;
        return new Result(pick(
                "I do not know that yet, " + name + ". What should I reply? (or say skip)",
                "New input. Teach me the reply, " + name + ", or say skip."));
    }
}
