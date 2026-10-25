package com.example.taptheball;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w=getWindow();
        w.setStatusBarColor(Color.rgb(7,10,24));
        w.setNavigationBarColor(Color.rgb(7,10,24));
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new SurvivorView(this));
    }

    static class SurvivorView extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random rng=new Random();
        final SharedPreferences prefs;
        final ArrayList<Enemy> enemies=new ArrayList<>();
        final ArrayList<Bullet> bullets=new ArrayList<>();
        final ArrayList<Gem> gems=new ArrayList<>();
        final ArrayList<Particle> particles=new ArrayList<>();
        final ArrayList<Star> stars=new ArrayList<>();
        final int bg=Color.rgb(7,10,24), cyan=Color.rgb(52,239,222), blue=Color.rgb(68,142,255), pink=Color.rgb(255,70,137), gold=Color.rgb(255,207,76), white=Color.WHITE;
        int screen=0; // menu, playing, paused, game over
        int score=0, best=0, level=1, kills=0, hp=3, xp=0, xpNeed=5, upgradeChoice=0;
        float px,py,playerR=17,joyX=0,joyY=0,joyBaseX=0,joyBaseY=0,joyKnobX=0,joyKnobY=0;
        boolean joyActive=false;
        long lastFrame=0;
        float spawnTimer=0,shotTimer=0,elapsed=0;
        float shake=0;
        String banner="";
        long bannerUntil=0;

        SurvivorView(Context c){
            super(c); prefs=c.getSharedPreferences("neon_survivor_save",Context.MODE_PRIVATE);
            best=prefs.getInt("best",0);
            for(int i=0;i<85;i++) stars.add(new Star(rng.nextFloat(),rng.nextFloat(),.5f+rng.nextFloat()*1.7f));
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        }

        void fill(Canvas c,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.clearShadowLayer();}
        void rect(Canvas c,float l,float t,float r,float b,int color){fill(c,color);c.drawRect(l,t,r,b,p);}
        void round(Canvas c,float l,float t,float r,float b,float rad,int color){fill(c,color);c.drawRoundRect(l,t,r,b,rad,rad,p);}
        void circle(Canvas c,float x,float y,float r,int color){fill(c,color);c.drawCircle(x,y,r,p);}
        void label(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){
            fill(c,color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(Typeface.create(bold?"sans-serif-black":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);
        }
        void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){
            fill(c,color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x1,y1,x2,y2,p);
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c); long now=System.currentTimeMillis();
            float dt=lastFrame==0?.016f:Math.min(.04f,(now-lastFrame)/1000f);lastFrame=now;
            int w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{Color.rgb(7,10,26),Color.rgb(18,16,47),Color.rgb(7,24,39)},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            for(Star s:stars){circle(c,s.x*w,s.y*h,s.r,Color.argb(s.a,130,190,255));if(screen==1)s.y=(s.y+.00025f*dt*60)%1f;}
            if(screen==0) drawMenu(c,w,h);
            else if(screen==1) { update(dt,now,w,h); drawWorld(c,w,h); drawHUD(c,w,h); }
            else if(screen==2) { drawWorld(c,w,h); drawPause(c,w,h); }
            else drawGameOver(c,w,h);
            drawParticles(c,dt);
            postInvalidateDelayed(16);
        }

        void logo(Canvas c,float x,float y,float s){
            p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*s);p.setColor(cyan);p.setShadowLayer(14*s,0,0,cyan);c.drawCircle(x,y,29*s,p);p.clearShadowLayer();
            p.setStyle(Paint.Style.FILL);p.setColor(blue);Path ship=new Path();ship.moveTo(x,y-22*s);ship.lineTo(x+17*s,y+15*s);ship.lineTo(x,y+8*s);ship.lineTo(x-17*s,y+15*s);ship.close();c.drawPath(ship,p);
            p.setColor(white);c.drawCircle(x,y-3*s,4*s,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*s);p.setColor(pink);c.drawArc(x-38*s,y-38*s,x+38*s,y+38*s,35,100,false,p);p.setStyle(Paint.Style.FILL);
        }

        void drawMenu(Canvas c,int w,int h){
            float cx=w/2f;
            logo(c,cx,h*.18f,1.35f);
            label(c,"NEON",cx,h*.18f+76,48,white,true,Paint.Align.CENTER);
            label(c,"SURVIVOR",cx,h*.18f+119,35,cyan,true,Paint.Align.CENTER);
            label(c,"2D SPACE SURVIVAL ARCADE",cx,h*.18f+148,12,Color.rgb(164,183,220),true,Paint.Align.CENTER);
            round(c,24,h*.37f,w-24,h*.37f+116,20,Color.argb(210,25,32,63));
            label(c,"SURVIVE THE SWARM",cx,h*.37f+31,17,gold,true,Paint.Align.CENTER);
            label(c,"Move with the left thumbstick",cx,h*.37f+60,14,white,false,Paint.Align.CENTER);
            label(c,"Your ship fires automatically",cx,h*.37f+83,14,white,false,Paint.Align.CENTER);
            label(c,"Collect energy • Level up • Stay alive",cx,h*.37f+103,12,Color.rgb(167,187,222),false,Paint.Align.CENTER);
            round(c,32,h*.58f,w-32,h*.58f+62,18,cyan);
            label(c,"▶  START MISSION",cx,h*.58f+39,19,bg,true,Paint.Align.CENTER);
            label(c,"BEST SCORE   "+best,cx,h*.58f+102,15,gold,true,Paint.Align.CENTER);
            label(c,"3 lives • Endless waves • Bosses every 5 levels",cx,h-40,11,Color.rgb(151,169,207),false,Paint.Align.CENTER);
        }

        void startGame(){
            screen=1;score=0;level=1;kills=0;hp=3;xp=0;xpNeed=5;elapsed=0;shake=0;
            enemies.clear();bullets.clear();gems.clear();particles.clear();
            px=getWidth()/2f;py=getHeight()*.67f;spawnTimer=0;shotTimer=0;lastFrame=System.currentTimeMillis();
            banner="MISSION START";bannerUntil=System.currentTimeMillis()+1400;joyActive=false;
        }

        void update(float dt,long now,int w,int h){
            elapsed+=dt;
            if(joyActive){
                px+=joyX*235*dt;py+=joyY*235*dt;
                px=Math.max(22,Math.min(w-22,px));py=Math.max(80,Math.min(h-90,py));
            }
            spawnTimer-=dt;
            if(spawnTimer<=0){spawnEnemy(w,h);spawnTimer=Math.max(.32f,1.05f-level*.055f);}
            shotTimer-=dt;
            if(shotTimer<=0 && !enemies.isEmpty()){
                Enemy nearest=null;float dist=Float.MAX_VALUE;
                for(Enemy e:enemies){float d=dist(px,py,e.x,e.y);if(d<dist){dist=d;nearest=e;}}
                if(nearest!=null){
                    float dx=nearest.x-px,dy=nearest.y-py,len=Math.max(1,(float)Math.hypot(dx,dy));
                    float vx=dx/len*430,vy=dy/len*430;
                    bullets.add(new Bullet(px,py,vx,vy));
                    if(level>=3){bullets.add(new Bullet(px,py,vx*.94f-vy*.22f,vy*.94f+vx*.22f));bullets.add(new Bullet(px,py,vx*.94f+vy*.22f,vy*.94f-vx*.22f));}
                    if(level>=7){bullets.add(new Bullet(px,py,vx*.78f-vy*.62f,vy*.78f+vx*.62f));bullets.add(new Bullet(px,py,vx*.78f+vy*.62f,vy*.78f-vx*.62f));}
                    shotTimer=Math.max(.12f,.28f-level*.012f);
                }
            }
            Iterator<Enemy> ei=enemies.iterator();
            while(ei.hasNext()){
                Enemy e=ei.next();float dx=px-e.x,dy=py-e.y,len=Math.max(1,(float)Math.hypot(dx,dy));
                e.x+=dx/len*e.speed*dt;e.y+=dy/len*e.speed*dt;
                if(dist(px,py,e.x,e.y)<playerR+e.r){ei.remove();hp--;shake=10;burst(px,py,pink,18);if(hp<=0){finish();return;}}
                else if(e.y>h+50||e.x< -50||e.x>w+50)ei.remove();
            }
            Iterator<Bullet> bi=bullets.iterator();
            while(bi.hasNext()){
                Bullet b=bi.next();b.x+=b.vx*dt;b.y+=b.vy*dt;b.life-=dt;
                boolean hit=false;
                Iterator<Enemy> ej=enemies.iterator();
                while(ej.hasNext()){Enemy e=ej.next();if(dist(b.x,b.y,e.x,e.y)<e.r+5){hit=true;e.hp--;burst(b.x,b.y,cyan,3);if(e.hp<=0){ej.remove();kills++;score+=10+level*2;gems.add(new Gem(e.x,e.y));burst(e.x,e.y,e.boss?gold:pink,e.boss?32:14);if(e.boss){score+=100;hp=Math.min(3,hp+1);level++;banner="BOSS DOWN! +100  +1 LIFE";bannerUntil=now+1800;}}break;}}
                if(hit||b.life<=0||b.x<0||b.x>w||b.y<0||b.y>h)bi.remove();
            }
            Iterator<Gem> gi=gems.iterator();
            while(gi.hasNext()){Gem g=gi.next();float d=dist(px,py,g.x,g.y);if(d<90){float dx=px-g.x,dy=py-g.y,len=Math.max(1,(float)Math.hypot(dx,dy));g.x+=dx/len*130*dt;g.y+=dy/len*130*dt;}if(d<playerR+g.r+5){gi.remove();xp++;score+=2;burst(g.x,g.y,cyan,7);if(xp>=xpNeed){xp-=xpNeed;xpNeed+=2;level++;banner="LEVEL "+level+"  •  DAMAGE UP";bannerUntil=now+1600;}}
            }
            if(shake>0)shake=Math.max(0,shake-dt*25);
        }

        float dist(float x1,float y1,float x2,float y2){return (float)Math.hypot(x2-x1,y2-y1);}
        void spawnEnemy(int w,int h){
            int edge=rng.nextInt(4);float x,y;
            if(edge==0){x=-20;y=rng.nextFloat()*h*.75f;}
            else if(edge==1){x=w+20;y=rng.nextFloat()*h*.75f;}
            else if(edge==2){x=rng.nextFloat()*w;y=65;}
            else{x=rng.nextFloat()*w;y=-20;}
            boolean boss=level%5==0 && kills>0 && kills%25==0 && rng.nextFloat()<.12f;
            enemies.add(new Enemy(x,y,boss?29:12+rng.nextFloat()*5,(boss?35:48+level*5),boss?gold:pink,boss,boss?Math.min(18,5+level):1));
        }

        void drawWorld(Canvas c,int w,int h){
            c.save();if(shake>0)c.translate((rng.nextFloat()-.5f)*shake,(rng.nextFloat()-.5f)*shake);
            for(Gem g:gems){circle(c,g.x,g.y,g.r+5,Color.argb(45,52,239,222));Path d=new Path();d.moveTo(g.x,g.y-g.r);d.lineTo(g.x+g.r,g.y);d.lineTo(g.x,g.y+g.r);d.lineTo(g.x-g.r,g.y);d.close();fill(c,cyan);c.drawPath(d,p);}
            for(Bullet b:bullets){p.setShader(new LinearGradient(b.x,b.y,b.x-b.vx*.035f,b.y-b.vy*.035f,cyan,Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawCircle(b.x,b.y,4,p);p.setShader(null);}
            for(Enemy e:enemies){circle(c,e.x,e.y,e.r+5,Color.argb(45,255,70,137));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(e.boss?3:2);p.setColor(e.color);c.drawCircle(e.x,e.y,e.r,p);p.setStyle(Paint.Style.FILL);circle(c,e.x,e.y,e.r*.55f,e.color);circle(c,e.x-e.r*.18f,e.y-e.r*.18f,3,white);if(e.boss){round(c,e.x-e.r,e.y-e.r-10,e.x+e.r,e.y-e.r-5,2,Color.rgb(65,28,50));round(c,e.x-e.r,e.y-e.r-10,e.x-e.r+2*e.r*(e.hp/(float)e.maxHp),e.y-e.r-5,2,gold);}}
            drawShip(c,px,py,playerR);
            c.restore();
        }

        void drawShip(Canvas c,float x,float y,float r){
            p.setShader(new RadialGradient(x,y,r*2,new int[]{Color.argb(90,52,239,222),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*2,p);p.setShader(null);
            Path ship=new Path();ship.moveTo(x,y-r*1.35f);ship.lineTo(x+r*.9f,y+r*.85f);ship.lineTo(x,y+r*.42f);ship.lineTo(x-r*.9f,y+r*.85f);ship.close();
            p.setShader(new LinearGradient(x-r,y-r,x+r,y+r,cyan,blue,Shader.TileMode.CLAMP));c.drawPath(ship,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(white);c.drawPath(ship,p);p.setStyle(Paint.Style.FILL);
            if(joyActive){circle(c,x,y+r*1.3f,3,pink);}
        }

        void drawHUD(Canvas c,int w,int h){
            round(c,12,12,w-12,64,16,Color.argb(215,18,25,52));
            label(c,"SCORE",25,31,9,Color.rgb(154,178,218),true,Paint.Align.LEFT);
            label(c,String.valueOf(score),25,53,20,white,true,Paint.Align.LEFT);
            label(c,"LV "+level,w*.49f,43,15,cyan,true,Paint.Align.CENTER);
            label(c,"❤ "+hp,w-65,43,17,pink,true,Paint.Align.CENTER);
            round(c,14,72,w-14,78,3,Color.rgb(42,54,85));
            round(c,14,72,14+(w-28)*(xp/(float)xpNeed),78,3,cyan);
            round(c,w-49,86,w-13,122,12,Color.argb(190,40,47,78));
            label(c,"Ⅱ",w-31,110,19,white,true,Paint.Align.CENTER);
            // virtual joystick
            circle(c,66,h-90,43,Color.argb(45,160,190,240));
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.argb(100,180,210,255));c.drawCircle(66,h-90,43,p);p.setStyle(Paint.Style.FILL);
            circle(c,66+(joyActive?joyX*22:0),h-90+(joyActive?joyY*22:0),20,Color.argb(105,52,239,222));
            label(c,"MOVE",66,h-31,9,Color.rgb(150,174,213),true,Paint.Align.CENTER);
            if(System.currentTimeMillis()<bannerUntil){round(c,w*.15f,h*.25f,w*.85f,h*.25f+42,14,Color.argb(220,23,35,67));label(c,banner,w/2f,h*.25f+27,16,gold,true,Paint.Align.CENTER);}
        }

        void drawPause(Canvas c,int w,int h){
            rect(c,0,0,w,h,Color.argb(185,3,5,18));label(c,"PAUSED",w/2f,h*.39f,36,white,true,Paint.Align.CENTER);
            round(c,32,h*.47f,w-32,h*.47f+58,16,cyan);label(c,"▶  RESUME",w/2f,h*.47f+37,18,bg,true,Paint.Align.CENTER);
            round(c,32,h*.47f+75,w-32,h*.47f+133,16,Color.rgb(41,49,80));label(c,"EXIT TO MENU",w/2f,h*.47f+112,16,white,true,Paint.Align.CENTER);
        }

        void drawGameOver(Canvas c,int w,int h){
            label(c,"MISSION FAILED",w/2f,h*.27f,30,white,true,Paint.Align.CENTER);
            label(c,""+score,w/2f,h*.38f,64,cyan,true,Paint.Align.CENTER);
            label(c,"FINAL SCORE",w/2f,h*.38f+28,12,Color.rgb(164,184,222),true,Paint.Align.CENTER);
            label(c,"BEST  "+best+"     LEVEL  "+level,w/2f,h*.46f,16,gold,true,Paint.Align.CENTER);
            round(c,32,h*.56f,w-32,h*.56f+60,18,cyan);label(c,"↻  TRY AGAIN",w/2f,h*.56f+38,19,bg,true,Paint.Align.CENTER);
            round(c,32,h*.56f+76,w-32,h*.56f+132,18,Color.rgb(37,45,77));label(c,"MAIN MENU",w/2f,h*.56f+111,16,white,true,Paint.Align.CENTER);
        }

        void finish(){screen=3;joyActive=false;if(score>best){best=score;prefs.edit().putInt("best",best).apply();}}
        void burst(float x,float y,int color,int count){for(int i=0;i<count;i++){double a=rng.nextDouble()*Math.PI*2;float sp=30+rng.nextFloat()*190;particles.add(new Particle(x,y,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,color,.35f+rng.nextFloat()*.5f));}}
        void drawParticles(Canvas c,float dt){Iterator<Particle> it=particles.iterator();while(it.hasNext()){Particle q=it.next();q.x+=q.vx*dt;q.y+=q.vy*dt;q.life-=dt;q.vx*=.97f;q.vy*=.97f;if(q.life<=0){it.remove();continue;}p.setColor(Color.argb((int)(Math.min(1,q.life*2)*220),Color.red(q.color),Color.green(q.color),Color.blue(q.color)));c.drawCircle(q.x,q.y,Math.max(1,q.life*5),p);}}

        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX(),y=e.getY();int w=getWidth(),h=getHeight();
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                if(screen==0){startGame();return true;}
                if(screen==3){if(y>h*.56f && y<h*.56f+70)startGame();else if(y>h*.56f+70)screen=0;invalidate();return true;}
                if(screen==2){if(y>h*.47f && y<h*.47f+65)screen=1;else if(y>h*.47f+70 && y<h*.47f+145){screen=0;enemies.clear();bullets.clear();gems.clear();}invalidate();return true;}
                if(x>w-65 && y<135){screen=2;joyActive=false;invalidate();return true;}
                if(x<w*.34f && y>h*.55f){joyActive=true;joyBaseX=66;joyBaseY=h-90;updateJoy(x,y);return true;}
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE && screen==1 && joyActive){updateJoy(x,y);return true;}
            if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL){joyActive=false;joyX=joyY=0;return true;}
            return true;
        }
        void updateJoy(float x,float y){float dx=x-joyBaseX,dy=y-joyBaseY;float len=(float)Math.hypot(dx,dy);if(len>1){joyX=dx/Math.max(35,len);joyY=dy/Math.max(35,len);}else{joyX=joyY=0;}}
        static class Star{float x,y,r;int a;Star(float x,float y,float r){this.x=x;this.y=y;this.r=r;a=45+(int)(Math.random()*125);}}
        static class Enemy{float x,y,r,speed;int color,hp,maxHp;boolean boss;Enemy(float x,float y,float r,float speed,int color,boolean boss,int hp){this.x=x;this.y=y;this.r=r;this.speed=speed;this.color=color;this.boss=boss;this.hp=hp;this.maxHp=hp;}}
        static class Bullet{float x,y,vx,vy,life=1.6f;Bullet(float x,float y,float vx,float vy){this.x=x;this.y=y;this.vx=vx;this.vy=vy;}}
        static class Gem{float x,y,r=6;Gem(float x,float y){this.x=x;this.y=y;}}
        static class Particle{float x,y,vx,vy,life;int color;Particle(float x,float y,float vx,float vy,int color,float life){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.color=color;this.life=life;}}
    }
}