package br.edu.motionbound.game;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayDeque;
import java.util.Arrays;

/** One shared breadth-first distance field per player cell, used to navigate around houses/walls. */
public final class DungeonNavigator {
    private final DungeonMap map;
    private final int[][] distance=new int[DungeonMap.WIDTH][DungeonMap.HEIGHT];
    private int goalX=-1,goalY=-1;
    private static final int[][] NEIGHBOURS={{1,0},{0,1},{-1,0},{0,-1}};
    public DungeonNavigator(DungeonMap map) {this.map=map;}
    public void updateGoal(float x,float y) {
        int gx=(int)(x/16),gy=(int)(y/16);if(gx==goalX&&gy==goalY)return;
        goalX=gx;goalY=gy;for(int[] row:distance)Arrays.fill(row,Integer.MAX_VALUE);
        if(!map.isFloor(gx,gy))return;
        ArrayDeque<int[]> queue=new ArrayDeque<>();queue.add(new int[]{gx,gy});distance[gx][gy]=0;
        while(!queue.isEmpty()) {
            int[] cell=queue.removeFirst();
            for(int[] delta:NEIGHBOURS) {
                int nx=cell[0]+delta[0],ny=cell[1]+delta[1];
                if(map.isFloor(nx,ny)&&distance[nx][ny]==Integer.MAX_VALUE) {
                    distance[nx][ny]=distance[cell[0]][cell[1]]+1;queue.add(new int[]{nx,ny});
                }
            }
        }
    }
    public Vector2 direction(float x,float y,float targetX,float targetY,Vector2 output) {
        if(CombatRules.clearPath(map,x,y,targetX,targetY))return output.set(targetX-x,targetY-y).nor();
        int cx=(int)(x/16),cy=(int)(y/16);output.setZero();
        if(!map.isFloor(cx,cy))return output;
        int best=distance[cx][cy],nx=cx,ny=cy;
        for(int[] delta:NEIGHBOURS) {
            int tx=cx+delta[0],ty=cy+delta[1];
            if(map.isFloor(tx,ty)&&distance[tx][ty]<best){best=distance[tx][ty];nx=tx;ny=ty;}
        }
        if(nx!=cx||ny!=cy) {
            // Centre the perpendicular axis before rounding a tight corner.
            float tx=nx*16+8,ty=ny*16+8;
            if(nx!=cx&&Math.abs(y-(cy*16+8))>2)tx=cx*16+8;
            if(ny!=cy&&Math.abs(x-(cx*16+8))>2)ty=cy*16+8;
            output.set(tx-x,ty-y).nor();
        }
        return output;
    }
}
