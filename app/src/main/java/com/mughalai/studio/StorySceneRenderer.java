package com.mughalai.studio;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Data-driven 2D scene composer.
 * Characters are selected from the current story beat; no default rabbit/bird
 * is inserted when a story names something else.
 */
public final class StorySceneRenderer {
    private StorySceneRenderer() {}

    private enum Actor {
        HUMAN, KING, PRINCESS, FARMER, SOLDIER,
        LION, ELEPHANT, MONKEY, BEAR, FOX, WOLF, DEER, HORSE, CAMEL,
        DOG, CAT, COW, GOAT, SHEEP, PIG,
        RABBIT, TURTLE, SNAKE, CROCODILE,
        EAGLE, PIGEON, SPARROW, PARROT, DUCK, CHICKEN,
        FISH, SHARK, WHALE, DOLPHIN
    }
    private enum Action { WALK, RUN, FLY, SWIM, EAT, SLEEP, FIGHT, HELP, BUILD, JUMP, ENTER, EXIT, CLAP, IDLE }
    private enum Place { FOREST, SEA, DESERT, CITY, HOUSE, FARM, PALACE, TRACK, SKY }

    public static void draw(Canvas c, String beat, int frame, boolean english) {
        String s = beat == null ? "" : beat.toLowerCase(Locale.ROOT);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Place place = place(s);
        Action action = action(s);
        drawBackground(c, p, place, frame, s);
        drawProps(c, p, s, place, frame);

        List<Actor> actors = actors(s);
        if (actors.isEmpty() && hasAny(s, "وہ", "اس نے", "آدمی", "شخص", "he ", "she ", "they ")) {
            actors.add(Actor.HUMAN);
        }
        int n = actors.size();
        for (int i = 0; i < n; i++) {
            float x = n == 1 ? 650 : 390 + i * (520f / Math.max(1, n - 1));
            float phase = frame * 0.18f + i * 1.7f;
            drawActor(c, p, actors.get(i), x, 475, action, phase, i);
        }
        drawCaption(c, p, beat, english);
    }

    private static List<Actor> actors(String s) {
        List<Actor> out = new ArrayList<>();
        add(out,s,Actor.KING,"بادشاہ","راجا","king");
        add(out,s,Actor.PRINCESS,"شہزادی","princess");
        add(out,s,Actor.FARMER,"کسان","کاشتکار","farmer");
        add(out,s,Actor.SOLDIER,"سپاہی","فوجی","soldier");
        add(out,s,Actor.ELEPHANT,"ہاتھی","elephant");
        add(out,s,Actor.LION,"شیر","lion");
        add(out,s,Actor.MONKEY,"بندر","monkey");
        add(out,s,Actor.BEAR,"ریچھ","bear");
        add(out,s,Actor.FOX,"لومڑی","fox");
        add(out,s,Actor.WOLF,"بھیڑیا","wolf");
        add(out,s,Actor.DEER,"ہرن","deer");
        add(out,s,Actor.HORSE,"گھوڑا","horse");
        add(out,s,Actor.CAMEL,"اونٹ","camel");
        add(out,s,Actor.DOG,"کتا","کتے","dog");
        add(out,s,Actor.CAT,"بلی","cat");
        add(out,s,Actor.COW,"گائے","cow");
        add(out,s,Actor.GOAT,"بکری","goat");
        add(out,s,Actor.SHEEP,"بھیڑ","sheep");
        add(out,s,Actor.PIG,"سور","pig");
        add(out,s,Actor.RABBIT,"خرگوش","rabbit","hare");
        add(out,s,Actor.TURTLE,"کچھوا","کچوا","turtle","tortoise");
        add(out,s,Actor.SNAKE,"سانپ","snake");
        add(out,s,Actor.CROCODILE,"مگرمچھ","crocodile","alligator");
        add(out,s,Actor.EAGLE,"عقاب","eagle");
        add(out,s,Actor.PIGEON,"کبوتر","pigeon");
        add(out,s,Actor.SPARROW,"چڑیا","sparrow");
        add(out,s,Actor.PARROT,"طوطا","parrot");
        add(out,s,Actor.DUCK,"بطخ","duck");
        add(out,s,Actor.CHICKEN,"مرغی","مرغا","chicken","hen","rooster");
        add(out,s,Actor.SHARK,"شارک","shark");
        add(out,s,Actor.WHALE,"وہیل","whale");
        add(out,s,Actor.DOLPHIN,"ڈولفن","dolphin");
        add(out,s,Actor.FISH,"مچھلی","مچھلیاں","fish");
        add(out,s,Actor.HUMAN,"لڑکا","لڑکی","آدمی","عورت","بچہ","بچوں","انسان","boy","girl","man","woman","child","children","person");
        return out;
    }

    private static void add(List<Actor> list, String s, Actor a, String... words) {
        for (String w : words) if (s.contains(w)) { if (!list.contains(a)) list.add(a); return; }
    }
    private static boolean hasAny(String s, String... words) {
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }

    private static Action action(String s) {
        if (hasAny(s,"اڑ","پرواز","flying"," fly","flew")) return Action.FLY;
        if (hasAny(s,"تیر","swim","swimming")) return Action.SWIM;
        if (hasAny(s,"سو گیا","سو گئی","سو رہا","سوتا","sleep","slept")) return Action.SLEEP;
        if (hasAny(s,"کھا","کھاتا","کھاتی","eat","eating","ate")) return Action.EAT;
        if (hasAny(s,"لڑ","حملہ","fight","fighting","attack","attacked")) return Action.FIGHT;
        if (hasAny(s,"مدد","بچایا","بچانے","help","helped","rescue")) return Action.HELP;
        if (hasAny(s,"تعمیر","گھونسلا","بناتا","بنا رہا","build","building","built")) return Action.BUILD;
        if (hasAny(s,"تالیاں","تالی","clap","applaud")) return Action.CLAP;
        if (hasAny(s,"چھلانگ","اچھل","jump","jumping")) return Action.JUMP;
        if (hasAny(s,"اندر داخل","گھس","داخل ہوا","enter","entered","crawl into")) return Action.ENTER;
        if (hasAny(s,"باہر نکلا","باہر نکل","exit","left")) return Action.EXIT;
        if (hasAny(s,"دوڑ","بھاگ","race","run","running","ran")) return Action.RUN;
        if (hasAny(s,"چل","جا رہا","آ رہا","walk","walking","went","goes")) return Action.WALK;
        return Action.IDLE;
    }

    private static Place place(String s) {
        if (hasAny(s,"سمندر","دریا","تالاب","پانی","sea","ocean","river","pond","water")) return Place.SEA;
        if (hasAny(s,"صحرا","ریگستان","ریت","desert")) return Place.DESERT;
        if (hasAny(s,"جنگل","درخت","forest","woods","tree")) return Place.FOREST;
        if (hasAny(s,"کھیت","فارم","گاؤں","farm","village","barn")) return Place.FARM;
        if (hasAny(s,"محل","قلعہ","بادشاہت","palace","castle")) return Place.PALACE;
        if (hasAny(s,"گھر","کمرہ","مکان","house","home","room")) return Place.HOUSE;
        if (hasAny(s,"شہر","بازار","سڑک","city","market","street")) return Place.CITY;
        if (hasAny(s,"دوڑ","ریس","race","track")) return Place.TRACK;
        if (hasAny(s,"آسمان","بادل","sky","cloud")) return Place.SKY;
        return Place.FOREST;
    }

    private static void drawBackground(Canvas c, Paint p, Place place, int frame, String s) {
        boolean night = hasAny(s,"رات","اندھیرا","چاند","night","moon","dark");
        int sky = night ? Color.rgb(20,28,65) : place == Place.SEA ? Color.rgb(95,185,225) : Color.rgb(130,205,238);
        c.drawColor(sky);
        p.setColor(night ? Color.rgb(245,240,195) : Color.rgb(255,220,70));
        c.drawCircle(1080,100,42,p);
        p.setColor(Color.argb(150,255,255,255));
        float cloud = (frame * 0.55f) % 1500 - 120;
        c.drawCircle(cloud,145,26,p); c.drawCircle(cloud+36,130,34,p); c.drawCircle(cloud+72,147,25,p);
        switch(place) {
            case SEA:
                p.setColor(Color.rgb(25,125,190)); c.drawRect(0,365,1280,720,p);
                p.setColor(Color.WHITE);
                for(int i=0;i<9;i++){ float x=(i*170+frame*2)%1320; c.drawLine(x,420+i%3*35,x+65,420+i%3*35,p); }
                break;
            case DESERT:
                p.setColor(Color.rgb(220,170,100)); c.drawRect(0,440,1280,720,p);
                p.setColor(Color.rgb(190,135,75)); c.drawOval(60,385,650,610,p); c.drawOval(650,405,1330,620,p);
                break;
            case CITY: case HOUSE: case PALACE:
                p.setColor(Color.rgb(90,145,90)); c.drawRect(0,490,1280,720,p);
                p.setColor(place==Place.PALACE?Color.rgb(235,210,165):Color.rgb(205,190,165));
                c.drawRect(130,245,480,510,p);
                p.setColor(Color.rgb(130,80,55)); c.drawRect(260,380,350,510,p);
                p.setColor(Color.rgb(175,65,55)); c.drawRect(105,220,505,275,p);
                if(place==Place.CITY){ c.drawRect(760,300,1030,510,p); c.drawRect(1080,350,1210,510,p); }
                break;
            default:
                p.setColor(Color.rgb(75,145,75)); c.drawRect(0,470,1280,720,p);
                for(int i=0;i<7;i++){float x=40+i*190; p.setColor(Color.rgb(105,70,40)); c.drawRect(x,300,x+24,535,p); p.setColor(Color.rgb(45,125,55)); c.drawCircle(x+12,285,75,p);}
        }
    }

    private static void drawProps(Canvas c, Paint p, String s, Place place, int frame) {
        if(hasAny(s,"سیب","apple")) {
            p.setColor(Color.rgb(100,65,35)); c.drawRect(910,285,950,520,p);
            p.setColor(Color.rgb(35,130,55)); c.drawCircle(930,260,125,p);
            p.setColor(Color.RED); c.drawCircle(870,240,15,p); c.drawCircle(970,300,15,p); c.drawCircle(925,180,15,p);
        }
        if(hasAny(s,"گھونسلا","گھونسلہ","nest")) {
            p.setColor(Color.rgb(120,75,35)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(8);
            for(int i=0;i<5;i++) c.drawArc(490+i*4,390+i*4,780-i*4,490-i*2,0,180,false,p);
            p.setStyle(Paint.Style.FILL); p.setStrokeWidth(1);
        }
        if(hasAny(s,"کشتی","boat","ship")) {
            p.setColor(Color.rgb(115,65,35)); Path b=new Path(); b.moveTo(820,390); b.lineTo(1130,390); b.lineTo(1050,470); b.lineTo(900,470); b.close(); c.drawPath(b,p);
        }
        if(hasAny(s,"غار","بل","سوراخ","cave","hole","burrow")) {
            p.setColor(Color.rgb(75,55,45)); c.drawOval(100,430,330,560,p);
        }
        if(hasAny(s,"گھر","مکان","house")) {
            p.setColor(Color.rgb(165,80,55)); Path roof=new Path(); roof.moveTo(100,270); roof.lineTo(300,120); roof.lineTo(500,270); roof.close(); c.drawPath(roof,p);
        }
    }

    private static void drawActor(Canvas c, Paint p, Actor a, float x, float ground, Action action, float phase, int index) {
        float move = action==Action.RUN || action==Action.WALK ? (float)Math.sin(phase)*38 : action==Action.JUMP ? Math.abs((float)Math.sin(phase))*75 : 0;
        float bob = action==Action.SLEEP ? 8 : (float)Math.sin(phase*2)*5;
        float y = ground - move - bob;
        int body = color(a);
        p.setColor(Color.argb(65,20,20,20)); c.drawOval(x-65,ground-8,x+65,ground+13,p);
        p.setColor(body);
        if(a==Actor.SNAKE) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(32); p.setStrokeCap(Paint.Cap.ROUND);
            Path path=new Path(); path.moveTo(x-115,ground-20); path.cubicTo(x-70,ground-100,x+10,ground+5,x+75,y-65); c.drawPath(path,p);
            p.setStyle(Paint.Style.FILL); p.setStrokeWidth(1); c.drawCircle(x+78,y-68,27,p);
            p.setColor(Color.BLACK); c.drawCircle(x+88,y-76,4,p); return;
        }
        if(a==Actor.FISH || a==Actor.SHARK || a==Actor.WHALE || a==Actor.DOLPHIN) {
            c.drawOval(x-105,y-65,x+95,y+55,p);
            Path tail=new Path(); tail.moveTo(x-85,y); tail.lineTo(x-145,y-48); tail.lineTo(x-140,y+48); tail.close(); c.drawPath(tail,p);
            p.setColor(Color.WHITE); c.drawCircle(x+55,y-20,9,p); p.setColor(Color.BLACK); c.drawCircle(x+58,y-20,4,p); return;
        }
        if(isBird(a)) {
            c.drawOval(x-58,y-35,x+55,y+35,p);
            float flap=(float)Math.sin(phase*3)*30;
            Path wing=new Path(); wing.moveTo(x-10,y); wing.quadTo(x-60,y-65-flap,x+30,y-30); wing.close(); c.drawPath(wing,p);
            c.drawCircle(x+38,y-43,23,p); p.setColor(Color.rgb(245,155,45));
            Path beak=new Path(); beak.moveTo(x+55,y-42); beak.lineTo(x+83,y-32); beak.lineTo(x+55,y-24); beak.close(); c.drawPath(beak,p);
            p.setColor(Color.BLACK); c.drawCircle(x+45,y-48,4,p); return;
        }
        if(isHuman(a)) {
            p.setColor(Color.rgb(235,185,145)); c.drawCircle(x,y-120,37,p);
            p.setColor(a==Actor.KING?Color.rgb(175,40,35):a==Actor.PRINCESS?Color.rgb(220,80,145):Color.rgb(40,85,160));
            c.drawRoundRect(x-42,y-82,x+42,y+30,18,18,p);
            if(a==Actor.KING){p.setColor(Color.rgb(245,195,45)); Path crown=new Path(); crown.moveTo(x-34,y-155); crown.lineTo(x-25,y-190); crown.lineTo(x,y-165); crown.lineTo(x+25,y-190); crown.lineTo(x+35,y-155); crown.close(); c.drawPath(crown,p);}
            limbs(c,p,x,y,phase,action); return;
        }
        // Animal body and head, with silhouette details selected by species.
        float scale = a==Actor.ELEPHANT?1.5f:a==Actor.HORSE||a==Actor.CAMEL?1.25f:1f;
        c.drawOval(x-70*scale,y-75*scale,x+70*scale,y+30*scale,p);
        c.drawCircle(x+48*scale,y-90*scale,43*scale,p);
        if(a==Actor.ELEPHANT) {
            p.setColor(body); c.drawOval(x+55,y-80,x+95,y+45,p);
            p.setColor(Color.rgb(220,185,165)); c.drawOval(x+20,y-120,x+65,y-55,p);
        } else if(a==Actor.LION || a==Actor.BEAR) {
            p.setColor(a==Actor.LION?Color.rgb(155,85,25):body); c.drawCircle(x+48*scale,y-90*scale,55*scale,p);
            p.setColor(body); c.drawCircle(x+48*scale,y-90*scale,35*scale,p);
        } else if(a==Actor.DEER || a==Actor.HORSE || a==Actor.CAMEL || a==Actor.GOAT) {
            p.setStrokeWidth(8); p.setColor(body);
            c.drawLine(x+35,y-125,x+18,y-175,p); c.drawLine(x+58,y-125,x+80,y-175,p); p.setStrokeWidth(1);
        } else if(a==Actor.CROCODILE) {
            c.drawOval(x-120,y-50,x+130,y+18,p);
        }
        p.setColor(Color.BLACK); c.drawCircle(x+62*scale,y-100*scale,6*scale,p);
        if(a==Actor.DOG || a==Actor.WOLF || a==Actor.FOX || a==Actor.CAT) {
            p.setColor(body); Path ear=new Path(); ear.moveTo(x+20,y-115); ear.lineTo(x+12,y-165); ear.lineTo(x+48,y-128); ear.close(); c.drawPath(ear,p);
        }
        p.setColor(Color.rgb(55,35,25));
        c.drawOval(x+65*scale,y-75*scale,x+90*scale,y-55*scale,p);
        limbs(c,p,x,y,phase,action);
        if(action==Action.EAT){p.setColor(Color.RED); c.drawCircle(x+120,y-30,15,p);}
        if(action==Action.SLEEP){p.setColor(Color.WHITE); p.setTextSize(30); c.drawText("Z",x+75,y-145,p);}
    }

    private static void limbs(Canvas c, Paint p, float x, float y, float phase, Action action) {
        float swing=(action==Action.RUN||action==Action.WALK)?(float)Math.sin(phase)*35:0;
        p.setStrokeWidth(15); p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x-35,y+10,x-45+swing,y+82,p); c.drawLine(x+35,y+10,x+45-swing,y+82,p);
        c.drawLine(x-42,y-60,x-78-swing,y-15,p); c.drawLine(x+42,y-60,x+78+swing,y-15,p);
        p.setStrokeWidth(1);
    }
    private static boolean isBird(Actor a){return a==Actor.EAGLE||a==Actor.PIGEON||a==Actor.SPARROW||a==Actor.PARROT||a==Actor.DUCK||a==Actor.CHICKEN;}
    private static boolean isHuman(Actor a){return a==Actor.HUMAN||a==Actor.KING||a==Actor.PRINCESS||a==Actor.FARMER||a==Actor.SOLDIER;}
    private static int color(Actor a) {
        switch(a) {
            case ELEPHANT: return Color.rgb(125,135,145);
            case LION: return Color.rgb(220,160,55);
            case MONKEY: case BEAR: return Color.rgb(125,78,45);
            case FOX: return Color.rgb(220,105,35);
            case WOLF: return Color.rgb(115,120,125);
            case DEER: case HORSE: case CAMEL: return Color.rgb(155,105,65);
            case DOG: return Color.rgb(165,115,70);
            case CAT: return Color.rgb(190,145,115);
            case COW: return Color.rgb(235,230,210);
            case GOAT: case SHEEP: return Color.rgb(220,215,195);
            case PIG: return Color.rgb(235,155,165);
            case RABBIT: return Color.rgb(225,220,205);
            case TURTLE: return Color.rgb(75,145,75);
            case SNAKE: case CROCODILE: return Color.rgb(65,135,70);
            case EAGLE: return Color.rgb(105,80,55);
            case PIGEON: return Color.rgb(155,165,180);
            case SPARROW: return Color.rgb(155,105,65);
            case PARROT: return Color.rgb(45,165,75);
            case DUCK: case CHICKEN: return Color.rgb(235,215,165);
            case SHARK: return Color.rgb(105,130,150);
            case WHALE: return Color.rgb(65,105,155);
            case DOLPHIN: case FISH: return Color.rgb(70,155,190);
            default: return Color.rgb(170,125,85);
        }
    }
    private static void drawCaption(Canvas c, Paint p, String beat, boolean english) {
        p.setColor(Color.argb(220,0,0,0)); c.drawRoundRect(35,570,1245,695,22,22,p);
        p.setColor(Color.WHITE); p.setTextSize(31); p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));
        String t=beat==null?"":beat.trim(); if(t.length()>75)t=t.substring(0,72)+"...";
        p.setTextAlign(english?Paint.Align.CENTER:Paint.Align.RIGHT);
        c.drawText(t,english?640:1205,642,p);
    }
}
