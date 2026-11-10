package br.edu.motionbound.ui;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/** Cosmetic state only: healing and resource costs are always applied by the game immediately. */
public final class HudFeedback {
    public static final float COLLECTION_SECONDS=.8f;
    public static final float AUTO_HEAL_SECONDS=4;
    public static final class HeartFlight {
        public final float startX,startY;
        public final int slot;
        public float age;
        HeartFlight(float x,float y,int slot){startX=x;startY=y;this.slot=slot;}
        public float progress(){return MathUtils.clamp((age-.14f)/(COLLECTION_SECONDS-.14f),0,1);}
    }
    public final Array<HeartFlight> flights=new Array<>();
    public final float[] heartPulse=new float[12],heartLost=new float[12];
    public float hurtPulse,healPulse,manaPulse,readyPulse,vigorPulse,lagMana;
    public float autoHealRemaining;
    public int autoHealCharges;
    private float hp,mana,stamina;
    private boolean initialized,magicReady;

    public void collect(float normalizedX,float normalizedY,float restoredHp) {
        if(flights.size==12)flights.removeIndex(0);
        flights.add(new HeartFlight(normalizedX,normalizedY,MathUtils.clamp((int)Math.ceil(restoredHp)-1,0,11)));
    }
    public void automaticHeal(int remaining){autoHealCharges=remaining;autoHealRemaining=AUTO_HEAL_SECONDS;healPulse=.3f;}
    public void reset(){flights.clear();initialized=false;hurtPulse=healPulse=manaPulse=readyPulse=vigorPulse=autoHealRemaining=0;
        autoHealCharges=0;
        java.util.Arrays.fill(heartPulse,0);java.util.Arrays.fill(heartLost,0);}
    public void update(GameUi.View v,float dt) {
        autoHealRemaining=Math.max(0,autoHealRemaining-dt);
        hurtPulse=Math.max(0,hurtPulse-dt);healPulse=Math.max(0,healPulse-dt);
        manaPulse=Math.max(0,manaPulse-dt);readyPulse=Math.max(0,readyPulse-dt);vigorPulse=Math.max(0,vigorPulse-dt);
        for(int i=0;i<heartPulse.length;i++){heartPulse[i]=Math.max(0,heartPulse[i]-dt);heartLost[i]=Math.max(0,heartLost[i]-dt);}
        if(initialized) {
            if(v.hp<hp){hurtPulse=.35f;for(int i=(int)v.hp;i<Math.ceil(hp)&&i<heartLost.length;i++)heartLost[i]=.35f;}
            if(v.hp>hp)healPulse=.3f;
            if(v.mana<mana-.1f)manaPulse=.45f;
            if(!magicReady&&v.magicCd<=0&&v.mana>=25)readyPulse=.5f;
            if((int)(v.stamina/12.5f)>(int)(stamina/12.5f))vigorPulse=.22f;
        } else {lagMana=v.mana;initialized=true;}
        hp=v.hp;mana=v.mana;stamina=v.stamina;magicReady=v.magicCd<=0&&v.mana>=25;
        lagMana=Math.max(v.mana,lagMana-dt*55);
        for(int i=flights.size-1;i>=0;i--) {
            HeartFlight flight=flights.get(i);flight.age+=dt;
            if(flight.age>=COLLECTION_SECONDS){heartPulse[flight.slot]=.45f;flights.removeIndex(i);}
        }
    }
}
