package br.edu.motionbound.game;

import com.badlogic.gdx.math.MathUtils;

/** A hand-designed, four-room dungeon. Tiles are 16x16 world pixels. */
public final class DungeonMap {
    public static final int TILE = 16;
    public static final int WIDTH = 55;
    public static final int HEIGHT = 35;
    public static final int TOWN = 1000;
    /** Wall sconces: x, y, and 0 for fire / 1 for an arcane flame. */
    public static final int[][] TORCHES = {{6,15,0},{15,15,0},{26,15,0},{46,15,0},
            {7,32,0},{23,32,1},{34,32,0},{49,32,1},{2,7,0},{19,12,0},
            {21,6,0},{50,7,0},{2,22,1},{27,29,0},{30,20,0},{52,26,1}};
    private final int[][] floor = new int[WIDTH][HEIGHT];
    private final int[][] props = new int[WIDTH][HEIGHT];

    public DungeonMap() {
        room(3, 3, 18, 14);
        room(22, 3, 49, 14);
        room(3, 18, 26, 31);
        room(31, 18, 51, 31);
        room(18, 8, 22, 10);
        room(10, 14, 12, 18);
        room(39, 14, 41, 18);
        room(26, 23, 31, 25);
        // Abandoned guard post: workbench, stools, storage and a worn training carpet.
        prop(5,12,72);prop(6,12,63);prop(5,11,73);prop(4,5,82);prop(5,5,82);
        prop(16,12,75);prop(17,12,64);prop(16,4,74);prop(4,9,TOWN+83);
        prop(14,5,66);prop(15,5,82);prop(4,13,TOWN+129);
        // Burial hall: paired headstones, stone sarcophagi and neglected offerings.
        for(int x:new int[]{25,30,35,44}) {prop(x,6,65);prop(x,11,65);}
        prop(47,12,54);prop(48,12,55);prop(47,4,82);prop(24,13,TOWN+129);
        prop(45,4,TOWN+129);prop(33,13,66);
        // Damp chapel: only small plants, mushrooms, pots and water from Tiny Town.
        prop(4,20,TOWN+17);prop(5,20,TOWN+29);prop(4,29,TOWN+17);
        prop(5,30,TOWN+29);prop(9,30,TOWN+17);prop(24,19,TOWN+17);
        prop(25,20,TOWN+29);prop(24,30,TOWN+17);prop(22,30,TOWN+129);
        prop(6,27,TOWN+130);prop(7,27,TOWN+129);prop(21,19,82);prop(22,19,82);
        prop(16,28,54);prop(17,28,55);prop(24,27,TOWN+29);
        // Reliquary: ceremonial furniture and an illuminated route to the exit chest.
        prop(33,20,64);prop(34,20,74);prop(50,20,75);prop(50,21,82);
        prop(33,30,65);prop(50,30,65);prop(36,29,54);prop(37,29,55);
        prop(42,20,TOWN+129);prop(43,20,TOWN+129);prop(50,25,66);
    }

    private void room(int x0, int y0, int x1, int y1) {
        for (int x=x0; x<=x1; x++) for (int y=y0; y<=y1; y++) floor[x][y]=1;
    }
    private void prop(int x, int y, int tileIndex) { props[x][y] = tileIndex; }
    public boolean isFloor(int x, int y) {
        return x>=0 && y>=0 && x<WIDTH && y<HEIGHT && floor[x][y]==1;
    }
    public int propAt(int x,int y) {
        return x<0 || y<0 || x>=WIDTH || y>=HEIGHT ? 0 : props[x][y];
    }
    public boolean canStand(float cx, float cy, float radius) {
        return isFloor(MathUtils.floor((cx-radius)/TILE),MathUtils.floor((cy-radius)/TILE))
            && isFloor(MathUtils.floor((cx+radius)/TILE),MathUtils.floor((cy-radius)/TILE))
            && isFloor(MathUtils.floor((cx-radius)/TILE),MathUtils.floor((cy+radius)/TILE))
            && isFloor(MathUtils.floor((cx+radius)/TILE),MathUtils.floor((cy+radius)/TILE));
    }
    public float tryX(float oldX, float y, float desiredX, float radius) {
        return canStand(desiredX,y,radius)?desiredX:oldX;
    }
    public float tryY(float x, float oldY, float desiredY, float radius) {
        return canStand(x,desiredY,radius)?desiredY:oldY;
    }
    public boolean decoration(int x,int y) {
        return ((x*43+y*79+x*y*13)%17)==1;
    }
    public int floorTile(int x,int y) {
        int hash=Math.floorMod(x*37+y*61+x*y*11,73);
        return hash==0?24:hash==1?12:0;
    }
    public int wallTile(int x,int y) {
        for(int[] torch:TORCHES) if(torch[0]==x&&torch[1]==y) return torch[2]==0?29:32;
        if(isFloor(x,y-1)) {
            if(Math.floorMod(x*7+y,13)==0)return 19;
            if(y>18&&Math.floorMod(x*3+y,9)==0)return 20;
            return (x%4==0)?28:40;
        }
        if(isFloor(x-1,y))return 58;
        if(isFloor(x+1,y))return 57;
        return 40;
    }
    public boolean overgrown(int x,int y) { return x>=3&&x<=26&&y>=18&&y<=31; }
    public boolean carpet(int x,int y) {
        return (x>=8&&x<=11&&y>=6&&y<=9)||(x>=43&&x<=48&&y>=24&&y<=28);
    }
    /** Decorations never alter walkability; existing saves and corridor widths remain valid. */
    public int detailAt(int x,int y) {
        if(carpet(x,y)) {
            boolean edgeX=!carpet(x-1,y)||!carpet(x+1,y);
            boolean edgeY=!carpet(x,y-1)||!carpet(x,y+1);
            if(edgeX&&edgeY)return 5;
            if(edgeX)return 4;
            if(edgeY)return 3;
            return (x==9&&y==8)||(x==46&&y==26)?6:2;
        }
        if(overgrown(x,y) && (!isFloor(x-1,y)||!isFloor(x+1,y)||!isFloor(x,y-1)||!isFloor(x,y+1)))return 1;
        return decoration(x,y)?0:-1;
    }
}
