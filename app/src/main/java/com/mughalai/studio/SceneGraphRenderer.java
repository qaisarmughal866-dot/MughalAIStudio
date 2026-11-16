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
 * Sentence-driven offline 2D scene renderer.
 * Only draws entities explicitly named in the current sentence; motion is
 * selected from verbs in that sentence. This is a procedural renderer, not
 * a generative video model.
 */
public final class SceneGraphRenderer {
    private SceneGraphRenderer() {}

    private enum Actor { RABBIT, TURTLE, SNAKE, PIGEON, SPARROW, LION,
        ELEPHANT, FOX, BEAR, MONKEY, HORSE, FISH, PRINCESS, PERSON }
    private enum Action { RUN, WALK, FLY, SWIM, SLEEP, EAT, JUMP, ENTER, HELP, CLAP, IDLE }

    private static final class ActorState {
        Actor actor; Action action; int index;
        ActorState(Actor a, Action m, int i) { actor=a; action=m; index=i; }
    }

    public static void draw(Canvas c, String sentence, int frame, boolean english) {
        String s = sentence == null ? "" : sentence.toLowerCase(Locale.ROOT);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        boolean night = has(s,"رات","چاند","night","moon");
        boolean sea = has(s,"سمندر","دریا","تالاب","sea","ocean","river");
        boolean desert = has(s,"صحرا","ریگستان","desert");
        boolean forest = has(s,"جنگل","درخت","forest","tree");
        boolean house = has(s,"گھر","مکان","محل","house","home","palace","محل");
        boolean market = has(s,"بازار","دکان","market","shop");
        int sky = night ? Color.rgb(17,27,62) : sea ? Color.rgb(115,195,225) :
                desert ? Color.rgb(240,194,126) : Color.rgb(130,198,235);
        c.drawColor(sky);
        p.setColor(night ? Color.rgb(245,239,190) : Color.rgb(255,218,65));
        c.drawCircle(1090,100,42,p);
        p.setColor(desert ? Color.rgb(205,155,91) : sea ? Color.rgb(45,137,190) : Color.rgb(78,153,82));
        c.drawRect(0,475,1280,720,p);
        if (sea) {
            p.setColor(Color.rgb(35,140,200)); c.drawRect(0,380,1280,565,p);
            p.setColor(Color.rgb(225,200,145)); c.drawRect(0,565,1280,720,p);
            p.setColor(Color.WHITE); p.setStrokeWidth(4);
            for(int i=0;i<9;i++){ float x=(i*167+frame*2)%1280; c.drawLine(x,425+(i%3)*35,x+65,425+(i%3)*35,p); }
        } else if (desert) {
            p.setColor(Color.rgb(215,165,100));
            c.drawOval(-100,420,620,650,p); c.drawOval(570,430,1380,680,p);
        } else if (forest) {
            for(int i=0;i<8;i++){ float x=55+i*175;
                p.setColor(Color.rgb(105,70,42)); c.drawRect(x-12,300,x+12,530,p);
                p.setColor(Color.rgb(45,125,55)); c.drawCircle(x,285,75,p);
                c.drawCircle(x-35,330,48,p); c.drawCircle(x+40,330,48,p);
            }
        } else if (house || market) {
            p.setColor(Color.rgb(230,211,174)); c.drawRect(115,260,485,520,p);
            p.setColor(Color.rgb(160,78,53));
            Path roof=new Path(); roof.moveTo(85,270); roof.lineTo(300,130); roof.lineTo(520,270); roof.close(); c.drawPath(roof,p);
            p.setColor(Color.rgb(105,70,45)); c.drawRect(255,385,345,520,p);
            if(market){ p.setColor(Color.rgb(220,65,55)); c.drawRect(540,290,940,325,p); }
        }

        List<ActorState> actors = new ArrayList<>();
        add(actors,s,Actor.RABBIT,Action.RUN, "خرگوش","rabbit");
        add(actors,s,Actor.TURTLE,Action.WALK, "کچھوا","کچوا","turtle","tortoise");
        add(actors,s,Actor.SNAKE,Action.WALK, "سانپ","snake");
        add(actors,s,Actor.PIGEON,Action.FLY, "کبوتر","pigeon");
        add(actors,s,Actor.SPARROW,Action.FLY, "چڑیا","sparrow");
        add(actors,s,Actor.LION,Action.WALK, "شیر","lion");
        add(actors,s,Actor.ELEPHANT,Action.WALK, "ہاتھی","elephant");
        add(actors,s,Actor.FOX,Action.WALK, "لومڑی","fox");
        add(actors,s,Actor.BEAR,Action.WALK, "ریچھ","bear");
        add(actors,s,Actor.MONKEY,Action.JUMP, "بندر","monkey");
        add(actors,s,Actor.HORSE,Action.RUN, "گھوڑا","horse");
        add(actors,s,Actor.FISH,Action.SWIM, "مچھلی","fish");
        add(actors,s,Actor.PRINCESS,Action.WALK, "شہزادی","princess");
        add(actors,s,Actor.PERSON,Action.WALK, "لڑکا","لڑکی","آدمی","عورت","بچہ","بچی","بادشاہ","شہزادہ","boy","girl","man","woman","child","king","prince");
        for(int i=0;i<actors.size();i++) drawActor(c,p,actors.get(i),i,actors.size(),frame,sea);

        // Props that are actually mentioned in the current sentence.
        if(has(s,"گھونسلا","گھونسلہ","nest")) {
            p.setColor(Color.rgb(115,72,38)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(13);
            c.drawArc(520,355,760,490,5,170,false,p); p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(5); for(int i=0;i<7;i++) c.drawLine(540+i*28,420,570+i*22,440,p);
        }
        if(has(s,"بل","سوراخ","کھوہ","burrow","hole")) {
            p.setColor(Color.rgb(85,57,39)); c.drawOval(860,455,1090,535,p);
            p.setColor(Color.rgb(35,27,23)); c.drawOval(900,462,1050,520,p);
        }
        if(has(s,"سیب","apple")) {
            p.setColor(Color.rgb(100,65,35)); c.drawRect(1000,280,1040,520,p);
            p.setColor(Color.rgb(40,130,55)); c.drawCircle(1020,260,95,p);
            p.setColor(Color.RED); for(int i=0;i<5;i++) c.drawCircle(970+(i%3)*45,235+(i/3)*65,13,p);
        }
        if(has(s,"کشتی","boat","ship")) {
            p.setColor(Color.rgb(125,72,40)); Path boat=new Path();
            boat.moveTo(780,405); boat.lineTo(1100,405); boat.lineTo(1030,475); boat.lineTo(850,475); boat.close(); c.drawPath(boat,p);
            p.setColor(Color.WHITE); c.drawRect(925,285,932,405,p);
            Path sail=new Path(); sail.moveTo(940,295); sail.lineTo(1050,390); sail.lineTo(940,390); sail.close(); c.drawPath(sail,p);
        }
        // Story sentence as subtitle.
        p.setColor(Color.argb(215,0,0,0)); c.drawRoundRect(35,570,1245,690,20,20,p);
        p.setColor(Color.WHITE); p.setTextSize(30); p.setTextAlign(english?Paint.Align.CENTER:Paint.Align.RIGHT);
        String caption=sentence==null?"":sentence.trim(); if(caption.length()>72) caption=caption.substring(0,69)+"...";
        c.drawText(caption,english?640:1205,640,p);
    }

    private static void add(List<ActorState> out,String s,Actor a,Action d,String... words) {
        if(!has(s,words)) return;
        Action m=d;
        if(has(s,"سو گیا","سو گئی","سو رہا","سو رہی","سویا","sleep","sleeping","asleep")) m=Action.SLEEP;
        else if(has(s,"کھا رہا","کھا رہی","کھاتا","کھاتی","کھایا","کھانے","eat","eating")) m=Action.EAT;
        else if(has(s,"اڑ","پرواز","اڑتا","اڑتی","fly","flying")) m=Action.FLY;
        else if(has(s,"تیر","تیرتا","تیرتی","swim","swimming")) m=Action.SWIM;
        else if(has(s,"دوڑ","بھاگ","ریس","run","running","race")) m=Action.RUN;
        else if(has(s,"چھلانگ","اچھل","jump","jumping")) m=Action.JUMP;
        else if(has(s,"چل","جا رہا","جا رہی","walk","walking","going")) m=Action.WALK;
        else if(has(s,"مدد","اٹھایا","سہارا","help","helping")) m=Action.HELP;
        else if(has(s,"تالیاں","تالی","clap","clapping")) m=Action.CLAP;
        out.add(new ActorState(a,m,out.size()));
    }

    private static boolean has(String s,String... words){ for(String w:words) if(s.contains(w)) return true; return false; }

    private static void drawActor(Canvas c,Paint p,ActorState st,int index,int count,int frame,boolean sea) {
        float phase=frame*0.16f+index*1.8f;
        float x=count==1?650:260+index*(760f/Math.max(1,count-1));
        float y=sea && st.actor==Actor.FISH?420:455;
        float move=(float)(st.action==Action.RUN?Math.sin(phase)*90:st.action==Action.WALK?Math.sin(phase)*38:st.action==Action.JUMP?Math.abs(Math.sin(phase))*65:0);
        x+=move;
        if(st.action==Action.FLY) y=270+(float)Math.sin(phase)*35;
        if(st.action==Action.SWIM) y=430+(float)Math.sin(phase)*20;
        if(st.action==Action.SLEEP) y+=25;
        float bob=(float)Math.sin(phase*2)*5;
        switch(st.actor) {
            case SNAKE:
                p.setColor(Color.rgb(65,145,60)); p.setStrokeWidth(28); p.setStrokeCap(Paint.Cap.ROUND);
                Path snake=new Path(); snake.moveTo(x-90,y+15); snake.cubicTo(x-45,y-50,x+20,y+50,x+70,y-5); c.drawPath(snake,p);
                p.setStrokeWidth(1); c.drawCircle(x+72,y-8,24,p); break;
            case FISH:
                p.setColor(Color.rgb(240,145,45)); c.drawOval(x-65,y-30,x+55,y+30,p);
                Path tail=new Path(); tail.moveTo(x-55,y); tail.lineTo(x-90,y-35); tail.lineTo(x-90,y+35); tail.close(); c.drawPath(tail,p); break;
            case TURTLE:
                p.setColor(Color.rgb(65,135,65)); c.drawOval(x-75,y-35+bob,x+75,y+40+bob,p);
                p.setColor(Color.rgb(105,85,45)); c.drawOval(x-48,y-58+bob,x+48,y+25+bob,p);
                c.drawCircle(x+72,y-18+bob,22,p); break;
            case ELEPHANT:
                p.setColor(Color.rgb(135,145,155)); c.drawOval(x-65,y-65+bob,x+55,y+55+bob,p);
                c.drawCircle(x+45,y-65+bob,40,p); c.drawOval(x+55,y-35+bob,x+90,y+65+bob,p);
                p.setColor(Color.WHITE); c.drawCircle(x+57,y-78+bob,7,p); break;
            case RABBIT:
                p.setColor(Color.rgb(190,155,125)); c.drawOval(x-48,y-38+bob,x+48,y+42+bob,p);
                c.drawCircle(x+35,y-45+bob,34,p); c.drawOval(x+10,y-120+bob,x+34,y-40+bob,p);
                c.drawOval(x+42,y-118+bob,x+65,y-38+bob,p); break;
            case PIGEON: case SPARROW:
                p.setColor(st.actor==Actor.PIGEON?Color.rgb(160,175,190):Color.rgb(160,105,60));
                c.drawOval(x-55,y-28,x+50,y+30,p); c.drawCircle(x+28,y-38,25,p);
                p.setColor(Color.rgb(245,155,45)); Path beak=new Path(); beak.moveTo(x+48,y-42); beak.lineTo(x+76,y-34); beak.lineTo(x+48,y-26); beak.close(); c.drawPath(beak,p);
                p.setColor(Color.rgb(90,70,55)); p.setStrokeWidth(9); c.drawLine(x-8,y-8,x-30,y-48+(float)Math.sin(phase*4)*24,p); break;
            case LION: case BEAR: case FOX: case MONKEY: case HORSE:
                int fur=st.actor==Actor.LION?Color.rgb(205,145,55):st.actor==Actor.FOX?Color.rgb(205,100,45):st.actor==Actor.BEAR?Color.rgb(115,75,48):st.actor==Actor.HORSE?Color.rgb(130,82,48):Color.rgb(145,95,55);
                p.setColor(fur); c.drawOval(x-60,y-45+bob,x+55,y+40+bob,p); c.drawCircle(x+38,y-50+bob,34,p);
                if(st.actor==Actor.LION){ for(int i=0;i<10;i++){double a=i*Math.PI/5;c.drawCircle(x+38+(float)Math.cos(a)*37,y-50+(float)Math.sin(a)*37,13,p);} }
                if(st.actor==Actor.FOX){ Path ears=new Path(); ears.moveTo(x+15,y-70); ears.lineTo(x+5,y-115); ears.lineTo(x+38,y-78); ears.close(); c.drawPath(ears,p);}
                break;
            case PRINCESS: case PERSON:
                p.setColor(Color.rgb(235,185,145)); c.drawCircle(x,y-65+bob, thirty(),p);
                p.setColor(st.actor==Actor.PRINCESS?Color.rgb(205,65,145):Color.rgb(55,85,175)); c.drawRect(x-30,y-30+bob,x+30,y+65+bob,p);
                p.setColor(Color.rgb(45,35,30)); c.drawRect(x-24,y+65+bob,x-8,y+112,p); c.drawRect(x+8,y+65+bob,x+24,y+112,p); break;
        }
        // Eyes and limb swing make the silhouettes read as living characters.
        p.setColor(Color.BLACK); c.drawCircle(x+45,y-53+bob,4,p);
        p.setColor(Color.rgb(45,35,30)); p.setStrokeWidth(10);
        float swing=(float)Math.sin(phase*2)*20;
        if(st.actor!=Actor.SNAKE && st.actor!=Actor.FISH && st.actor!=Actor.PIGEON && st.actor!=Actor.SPARROW)
            for(int side=-1;side<=1;side+=2) c.drawLine(x+side*24,y+25+bob,x+side*30+swing*side,y+75+bob,p);
        p.setStrokeWidth(1);
    }
    private static float thirty(){ return 30f; }
}
