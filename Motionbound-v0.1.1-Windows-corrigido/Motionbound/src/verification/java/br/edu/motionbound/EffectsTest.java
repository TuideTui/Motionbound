package br.edu.motionbound;

import br.edu.motionbound.game.*;
import com.badlogic.gdx.math.Vector2;

public final class EffectsTest {
    public static void main(String[] args) {
        for(PotionEffect type:new PotionEffect[]{PotionEffect.FIRE,PotionEffect.POISON}) {
            StatusEffects effect=new StatusEffects();require(effect.apply(type),"First dose starts");
            require(effect.advance(.75f)==0,"No immediate periodic damage");
            require(!effect.apply(type)&&!effect.apply(type==PotionEffect.FIRE?PotionEffect.POISON:PotionEffect.FIRE),"No overlapping damaging doses");
            require(effect.advance(.25f)==.5f,"First tick is exactly half a heart");
            require(effect.advance(3)==1.5f&&effect.ticksRemaining==0,"Four ticks total two hearts");
            require(effect.advance(100)==0,"Expired effects cannot keep damaging");
            effect.apply(type);require(effect.advance(20)==2,"A long step never exceeds the complete dose");
        }
        StatusEffects effect=new StatusEffects();effect.apply(PotionEffect.SLOW);
        require(effect.movementScale()==.6f,"Grey vial slows movement");effect.advance(4);require(effect.movementScale()==1,"Slow expires");
        effect.apply(PotionEffect.FIRE);effect.advance(.6f);
        StatusEffects restored=new StatusEffects();restored.copyFrom(effect);
        require(restored.advance(.39f)==0&&restored.advance(.02f)==.5f,"Restored dose keeps its tick phase");
        effect.cleanseDamage();require(effect.advance(10)==0,"Healing removes periodic damage");
        require(effect.apply(PotionEffect.SLOW),"Slow may be acquired after expiring");effect.advance(2);
        require(!effect.apply(PotionEffect.SLOW)&&effect.slowRemaining==2,"An active slow is not refreshed by standing in fog");
        effect.advance(2);require(effect.apply(PotionEffect.SLOW),"Fog can slow again only after the previous slow ends");
        require(effect.apply(PotionEffect.RIFT),"First teleport starts arrival protection");effect.advance(.5f);
        require(!effect.apply(PotionEffect.RIFT)&&effect.riftRemaining<1,"Rifts cannot restart an unfinished arrival");
        effect.advance(1);require(effect.apply(PotionEffect.RIFT),"Teleport can be acquired again after arrival");
        DungeonMap map=new DungeonMap(2);DungeonNavigator navigator=new DungeonNavigator(map);
        float x=29*16+8,y=23*16+8;navigator.updateGoal(map.startX(),map.startY());Vector2 direction=new Vector2();
        for(int i=0;i<3000&&Vector2.dst2(x,y,map.startX(),map.startY())>8*8;i++) {
            navigator.direction(x,y,map.startX(),map.startY(),direction);
            x=map.tryX(x,y,x+direction.x*1.5f,5);y=map.tryY(x,y,y+direction.y*1.5f,5);
            require(map.canStand(x,y,5),"Navigation respects buildings");
        }
        require(Vector2.dst2(x,y,map.startX(),map.startY())<=8*8,"Navigation reaches the player around the village");
        System.out.println("PASS: half-heart doses, non-stacking effects, duration, cleansing and obstacle navigation");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
