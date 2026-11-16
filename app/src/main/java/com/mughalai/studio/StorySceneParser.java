package com.mughalai.studio;

import java.util.Locale;

/**
 * Small offline Urdu/English story parser.
 * It extracts reusable scene concepts without network access or paid APIs.
 * It is intentionally rule-based, not a general-purpose language model.
 */
public final class StorySceneParser {
    public enum Entity { SNAKE, RABBIT, TURTLE, PIGEON, SPARROW, LION, PRINCESS }
    public enum Action { ENTER, EXIT, RUN, FLY, BUILD, HELP, CLAP, EAT, SLEEP, SWIM }
    public enum Place { BURROW, NEST, FOREST, SEA, DESERT, HOUSE, RACE_TRACK }

    public static final class ScenePlan {
        public final String source;
        private ScenePlan(String source) { this.source = source; }
        public boolean has(Entity e) {
            switch (e) {
                case SNAKE: return hasAny(source,"سانپ","snake");
                case RABBIT: return hasAny(source,"خرگوش","rabbit");
                case TURTLE: return hasAny(source,"کچھوا","کچوا","turtle","tortoise");
                case PIGEON: return hasAny(source,"کبوتر","pigeon");
                case SPARROW: return hasAny(source,"چڑیا","sparrow");
                case LION: return hasAny(source,"شیر","lion");
                case PRINCESS: return hasAny(source,"شہزادی","princess");
                default: return false;
            }
        }
        public boolean does(Action a) {
            switch (a) {
                case ENTER: return hasAny(source,"داخل","گھس","اندر","enter","crawl into","going in");
                case EXIT: return hasAny(source,"باہر نکل","نکلتا","exit","leave");
                case RUN: return hasAny(source,"دوڑ","ریس","بھاگ","race","running","run");
                case FLY: return hasAny(source,"اڑ","پرواز","fly","flying");
                case BUILD: return hasAny(source,"بناتا","بنا رہی","بنا رہا","تعمیر","build","building");
                case HELP: return hasAny(source,"مدد","سہارا","help","helping");
                case CLAP: return hasAny(source,"تالیاں","تالی","clap","applaud","cheer");
                case EAT: return hasAny(source,"کھا","کھاتا","کھاتی","eat","eating");
                case SLEEP: return hasAny(source,"سو","سوتا","سوتی","sleep","sleeping");
                case SWIM: return hasAny(source,"تیر","تیرتا","swim","swimming");
                default: return false;
            }
        }
        public boolean at(Place p) {
            switch (p) {
                case BURROW: return hasAny(source,"بل","سوراخ","کھوہ","hole","burrow");
                case NEST: return hasAny(source,"گھونسلا","گھونسلہ","گھونسلے","nest","nesting");
                case FOREST: return hasAny(source,"جنگل","درخت","forest","tree");
                case SEA: return hasAny(source,"سمندر","دریا","sea","ocean");
                case DESERT: return hasAny(source,"صحرا","ریگ","desert");
                case HOUSE: return hasAny(source,"گھر","مکان","house","home");
                case RACE_TRACK: return does(Action.RUN);
                default: return false;
            }
        }
    }

    private StorySceneParser() { }

    public static ScenePlan parse(String story) {
        return new ScenePlan(story == null ? "" : story.toLowerCase(Locale.ROOT));
    }

    private static boolean hasAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
}
