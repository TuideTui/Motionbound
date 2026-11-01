package br.edu.motionbound.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The campaign changes encounter density, never the player's physical input thresholds. */
public record CampaignLevel(int number,String title,String difficulty,int enemyCount,Theme theme) {
    public enum Theme { CRYPT, VILLAGE, FARM, SNOW, FORTRESS }
    public record Spawn(int tileX,int tileY,int type) { }
    public static final int COUNT=5;
    public static final float INTRO_SECONDS=4.2f;
    private static final List<CampaignLevel> LEVELS=List.of(
        new CampaignLevel(1,"A Cripta Esquecida","Fácil",7,Theme.CRYPT),
        new CampaignLevel(2,"Vila das Lanternas","Moderado",10,Theme.VILLAGE),
        new CampaignLevel(3,"Campos Corrompidos","Intermediário",14,Theme.FARM),
        new CampaignLevel(4,"Passagem Congelada","Difícil",18,Theme.SNOW),
        new CampaignLevel(5,"Fortaleza do Crepúsculo","Muito difícil",22,Theme.FORTRESS)
    );
    public static CampaignLevel of(int number) {
        if(number<1||number>COUNT)throw new IllegalArgumentException("Fase inválida: "+number);
        return LEVELS.get(number-1);
    }
    public boolean last() {return number==COUNT;}
    public List<Spawn> spawns(DungeonMap map) {
        if(number==1)return List.of(new Spawn(13,12,0),new Spawn(30,8,0),new Spawn(39,12,1),
                new Spawn(8,27,0),new Spawn(20,21,1),new Spawn(38,23,0),new Spawn(46,26,1));
        List<Spawn> candidates=new ArrayList<>(),selected=new ArrayList<>();
        for(int y=5;y<=29;y+=3)for(int x=5;x<=50;x+=3) {
            int dx=x-map.startTileX,dy=y-map.startTileY;
            if(dx*dx+dy*dy>=64&&map.canStand(x*16+8,y*16+8,5)
                    &&Math.abs(x-map.exitTileX)+Math.abs(y-map.exitTileY)>2)
                candidates.add(new Spawn(x,y,0));
        }
        candidates.sort(Comparator.comparingInt(s->Math.floorMod(s.tileX()*73856093^s.tileY()*19349663^number*83492791,100003)));
        for(Spawn candidate:candidates) {
            if(selected.size()==enemyCount)break;
            boolean separated=true;
            for(Spawn other:selected) {
                int dx=candidate.tileX()-other.tileX(),dy=candidate.tileY()-other.tileY();
                if(dx*dx+dy*dy<9){separated=false;break;}
            }
            if(separated)selected.add(new Spawn(candidate.tileX(),candidate.tileY(),selected.size()%5<(number-1)?1:0));
        }
        if(selected.size()!=enemyCount)throw new IllegalStateException("Sem espaço para os inimigos da fase "+number);
        return List.copyOf(selected);
    }
}
