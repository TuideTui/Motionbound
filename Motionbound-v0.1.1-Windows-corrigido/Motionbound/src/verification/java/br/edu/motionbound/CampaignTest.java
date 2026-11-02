package br.edu.motionbound;

import br.edu.motionbound.game.CampaignLevel;
import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.SaveData;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Offline checks for reachability, safe encounters and stage-specific checkpoints. */
public final class CampaignTest {
    public static void main(String[] args) {
        int previousCount=0;Set<Integer> layouts=new HashSet<>();
        for(int stage=1;stage<=CampaignLevel.COUNT;stage++) {
            DungeonMap map=new DungeonMap(stage);CampaignLevel level=map.level;
            require(level.enemyCount()>previousCount,"Increasing encounter count");previousCount=level.enemyCount();
            require(map.canStand(map.startX(),map.startY(),5)&&map.canStand(map.exitX(),map.exitY(),5),"Safe start and exit: "+stage);
            boolean[][] visited=new boolean[DungeonMap.WIDTH][DungeonMap.HEIGHT];
            ArrayDeque<int[]> queue=new ArrayDeque<>();queue.add(new int[]{map.startTileX,map.startTileY});visited[map.startTileX][map.startTileY]=true;
            while(!queue.isEmpty()) {
                int[] cell=queue.removeFirst();
                for(int[] delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    int x=cell[0]+delta[0],y=cell[1]+delta[1];
                    if(map.isFloor(x,y)&&!visited[x][y]){visited[x][y]=true;queue.add(new int[]{x,y});}
                }
            }
            int layout=1;
            for(int x=0;x<DungeonMap.WIDTH;x++)for(int y=0;y<DungeonMap.HEIGHT;y++) {
                if(map.isFloor(x,y))require(visited[x][y],"No unreachable walkable islands: "+stage+" at "+x+","+y);
                if(stage>=2&&map.isFloor(x,y))require(map.propAt(x,y)!=29,"Wall torches never stand on the ground");
                layout=31*layout+(map.isFloor(x,y)?1:0);
                for(int tile:new int[]{map.floorTile(x,y),map.wallTile(x,y),map.propAt(x,y)})
                    require(tile>=0&&(tile<1000?tile<132:tile%1000<132),"Valid atlas index");
            }
            require(layouts.add(layout),"Each stage has its own layout");
            require(visited[map.exitTileX][map.exitTileY],"Exit reachable");
            if(map.outdoors())require(map.torches.length==0,"No floating outdoor torch glows");
            var spawns=level.spawns(map);Set<String> occupied=new HashSet<>();
            require(spawns.size()==level.enemyCount(),"All encounters created");
            SaveData checkpoint=new SaveData();checkpoint.stage=stage;checkpoint.x=map.startX();checkpoint.y=map.startY();
            checkpoint.hp=6;checkpoint.heals=3;checkpoint.stamina=100;checkpoint.mana=100;checkpoint.introRemaining=CampaignLevel.INTRO_SECONDS;
            for(var spawn:spawns) {
                require(occupied.add(spawn.tileX()+","+spawn.tileY()),"Unique enemy positions");
                require(visited[spawn.tileX()][spawn.tileY()]&&map.canStand(spawn.tileX()*16+8,spawn.tileY()*16+8,5),"Enemy reachable and clear of obstacles");
                int dx=spawn.tileX()-map.startTileX,dy=spawn.tileY()-map.startTileY;
                require(dx*dx+dy*dy>=36,"Safe starting area");
                SaveData.EnemyData enemy=new SaveData.EnemyData();enemy.x=spawn.tileX()*16+8;enemy.y=spawn.tileY()*16+8;
                enemy.type=spawn.type();enemy.hp=br.edu.motionbound.game.EnemyKind.of(enemy.type).maxHp;checkpoint.enemies.add(enemy);
            }
            SaveData loaded=SaveData.decode(checkpoint.encode());
            require(loaded.stage==stage&&loaded.enemies.size==level.enemyCount()&&loaded.introRemaining==CampaignLevel.INTRO_SECONDS,"Stage and countdown round-trip");
            checkpoint.stageComplete=true;reject(checkpoint);checkpoint.stageComplete=false;
            checkpoint.enemies.clear();checkpoint.defeated=level.enemyCount();checkpoint.x=map.exitX();checkpoint.y=map.exitY();
            checkpoint.stageComplete=true;checkpoint.introRemaining=0;
            require(SaveData.decode(checkpoint.encode()).stageComplete,"Completed stage round-trip");
        }
        System.out.println("PASS: five distinct connected maps, safe spawns, increasing encounters and stage checkpoints");
    }
    private static void reject(SaveData checkpoint) {try {checkpoint.validate();throw new AssertionError("Invalid checkpoint accepted");}catch(IllegalArgumentException expected){}}
    private static void require(boolean valid,String message){if(!valid)throw new AssertionError(message);}
}
