package br.edu.motionbound.game;

import com.badlogic.gdx.math.MathUtils;

/** Five deterministic maps, sharing 16px cells and the same collision/mini-map grid. */
public final class DungeonMap {
    public static final int TILE = 16;
    public static final int WIDTH = 55;
    public static final int HEIGHT = 35;
    public static final int TOWN = 1000, FARM=2000, SKI=3000;
    public final CampaignLevel level;
    public final int startTileX,startTileY,exitTileX,exitTileY;
    public final int[][] torches;
    /** Wall sconces: x, y, and 0 for fire / 1 for an arcane flame. */
    public static final int[][] TORCHES = {{6,15,0},{15,15,0},{26,15,0},{46,15,0},
            {7,32,0},{23,32,1},{34,32,0},{49,32,1},{2,7,0},{19,12,0},
            {21,6,0},{50,7,0},{2,22,1},{27,29,0},{30,20,0},{52,26,1}};
    private final int[][] floor = new int[WIDTH][HEIGHT];
    private final int[][] props = new int[WIDTH][HEIGHT];
    private final int[][] ground = new int[WIDTH][HEIGHT];

    public DungeonMap() {this(1);}
    public DungeonMap(int stage) {
        level=CampaignLevel.of(stage);
        startTileX=stage==1||stage==5?7:6;
        startTileY=stage==1?8:stage==3?7:stage==4?5:6;
        exitTileX=stage==2||stage==3?48:47;
        exitTileY=stage==1||stage==3?27:stage==4?29:28;
        int[][] wallLights=stage==1?TORCHES:stage==5?new int[][]{{6,14,0},{16,14,0},{24,13,0},{48,13,0},{10,32,1},{30,32,1},{48,32,1},{24,20,0}}
                :new int[0][];
        switch(level.theme()) {
            case CRYPT -> crypt();
            case VILLAGE -> village();
            case FARM -> farm();
            case SNOW -> snow();
            case FORTRESS -> fortress();
        }
        torches=stage==1?wallLights:java.util.Arrays.stream(wallLights)
                .filter(light->!isFloor(light[0],light[1])&&isFloor(light[0],light[1]-1)).toArray(int[][]::new);
    }
    private void crypt() {
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

    private void village() {
        room(3,3,51,31);
        // A residential crescent faces a market square, with a southern cottage and gatehouse.
        house(8,29,6,7,false,true);house(14,27,5,5,true,true);
        house(26,29,6,5,false,false);house(39,29,6,5,true,false);
        house(9,15,7,5,false,false);house(26,13,6,5,true,true);
        gatehouse(41,19);

        // Narrow cobbled lanes, connected to the doorsteps rather than cutting through houses.
        lane(6,6,6,8);lane(6,8,45,8);lane(21,8,21,23);lane(11,21,48,21);
        lane(11,21,11,22);lane(16,21,16,22);lane(29,21,29,24);
        lane(42,21,42,24);lane(48,21,48,28);lane(12,8,12,10);
        lane(37,8,37,21);lane(37,10,45,10);lane(45,10,45,11);
        lane(21,18,37,18);lane(6,8,6,13);
        for(int x=23;x<=33;x++)for(int y=15;y<=22;y++)
            if(Math.abs(x-28)+Math.abs(y-19)<=5)ground[x][y]=TOWN+43;

        // A sheltered apiary adjoining the western cottage, with a clear entrance to the south.
        fence(4,11,8,16,6,11);
        obstacle(5,13,TOWN+94);obstacle(7,14,TOWN+94);
        // Small gardens soften the buildings; they don't block the front doors or paths.
        garden(8,21,10,22);garden(17,21,18,22);garden(26,23,27,24);garden(30,23,31,24);
        garden(39,23,40,24);garden(43,23,44,24);garden(26,7,27,8);garden(30,7,31,8);

        // Village square: well, handcart, sign and a pair of decorative residents.
        obstacle(28,20,TOWN+104);
        obstacle(32,19,TOWN+57);prop(34,20,TOWN+83);
        prop(24,19,FARM+108);prop(34,16,FARM+109);
        prop(18,12,TOWN+130);prop(24,11,FARM+84);
        fence(45,26,50,30,48,26);
        for(int[] p:new int[][]{{46,28},{49,29},{46,29},{49,27}})ground[p[0]][p[1]]=TOWN+2;

        // Irregular groves form green neighbourhood edges instead of a rectangular empty lawn.
        for(int[] p:new int[][]{{4,22},{5,24},{7,25},{5,28},{6,30},{20,28},{22,29},
                {34,26},{35,28},{37,30},{49,5},{48,7},{50,10},{4,7},{17,5},{34,5},{39,5}})
            obstacle(p[0],p[1],TOWN+(p[0]%3==0?5:28));
        for(int x=3;x<=6;x++)for(int y=29;y<=31;y++)if(x+y>34)obstacle(x,y,TOWN+5);
        grove();
    }
    private void house(int left,int top,int width,int height,boolean redRoof,boolean paleWalls) {
        int roof=redRoof?52:48,wall=paleWalls?76:72;
        for(int dx=0;dx<width;dx++)for(int dy=0;dy<height;dy++) {
            int tile;
            if(dy<2)tile=roof+dy*12+(dx==0?0:dx==width-1?2:1);
            else tile=wall+(dx==0?0:dx==width-1?3:1);
            obstacle(left+dx,top-dy,TOWN+tile);
        }
        prop(left+width-2,top,TOWN+roof+3);prop(left+width-2,top-1,TOWN+roof+15);
        int window=paleWalls?88:84,door=paleWalls?89:85;
        prop(left+1,top-2,TOWN+window);prop(left+width-2,top-2,TOWN+window);
        if(height>=6) {
            prop(left+1,top-height+1,TOWN+window);
            prop(left+1,top-4,TOWN+window);prop(left+width-2,top-4,TOWN+window);
        }
        prop(left+width/2,top-height+1,TOWN+door);
    }
    private void gatehouse(int left,int top) {
        // Two three-tile towers flank the matching two-tile arch supplied by Tiny Town.
        for(int dx=0;dx<8;dx++)for(int dy=0;dy<7;dy++) {
            boolean tower=dx<3||dx>=5;
            if(!tower&&dy<2)continue;
            int column=dx<3?dx:dx>=5?dx-5:1;
            int tile=(dy==(tower?0:2)?96:108)+column;
            obstacle(left+dx,top-dy,TOWN+tile);
        }
        prop(left+1,top-2,TOWN+125);prop(left+6,top-2,TOWN+125);
        prop(left+3,top-3,TOWN+102);prop(left+4,top-3,TOWN+103);
        for(int dx=0;dx<2;dx++)for(int dy=0;dy<2;dy++)prop(left+3+dx,top-5-dy,TOWN+111+dx+dy*12);
        for(int x=left-1;x<=left+8;x++)ground[x][top-7]=TOWN+43;
    }
    private void lane(int x0,int y0,int x1,int y1) {
        for(int x=Math.min(x0,x1);x<=Math.max(x0,x1);x++)
            for(int y=Math.min(y0,y1);y<=Math.max(y0,y1);y++)ground[x][y]=TOWN+43;
    }
    private void garden(int x0,int y0,int x1,int y1) {
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)ground[x][y]=TOWN+2;
    }
    private void fence(int x0,int y0,int x1,int y1,int gateX,int gateY) {
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)
            if((x==x0||x==x1||y==y0||y==y1)&&(x!=gateX||y!=gateY))
                obstacle(x,y,TOWN+(x==x0||x==x1?59:45));
    }
    private void farm() {
        room(3,3,51,31);
        building(7,26,TOWN+48,4,3);building(45,19,FARM+90,3,3);
        field(13,6,22,15,42);field(31,6,40,15,66);
        field(13,22,22,29,30);field(31,22,40,29,53);
        for(int y=6;y<=28;y+=4) {prop(25,y,FARM+72);prop(42,y,FARM+84);}
        prop(9,20,TOWN+130);prop(10,20,FARM+73);prop(47,22,FARM+85);
        grove();
    }
    private void snow() {
        room(3,3,24,12);room(17,9,38,22);room(30,20,51,31);
        room(9,10,20,17);room(35,17,44,24);
        for(int x=8;x<=20;x+=4)obstacle(x,10,SKI+6);
        for(int y=14;y<=19;y+=3)obstacle(23,y,SKI+18);
        for(int x=35;x<=44;x+=3)obstacle(x,26,SKI+6);
        prop(6,8,TOWN+83);prop(32,20,66);prop(45,30,65);
        for(int x=0;x<WIDTH;x++)for(int y=0;y<HEIGHT;y++)
            if(!isFloor(x,y)&&Math.floorMod(x*19+y*7,5)==0)prop(x,y,SKI+6);
    }
    private void fortress() {
        room(3,3,17,13);room(22,3,50,12);room(3,18,20,31);room(25,17,51,31);
        room(15,7,25,10);room(10,11,13,20);room(18,23,28,26);room(39,10,42,20);room(21,12,34,20);
        for(int x:new int[]{28,34,45}) {obstacle(x,7,65);obstacle(x,25,65);}
        for(int y:new int[]{20,27}) {obstacle(8,y,54);obstacle(9,y,55);}
        prop(6,5,82);prop(15,5,82);prop(27,18,64);prop(31,18,74);
        prop(45,29,54);prop(46,29,55);prop(49,29,75);prop(49,19,66);
    }
    private void field(int x0,int y0,int x1,int y1,int crop) {
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++) {
            if((x==x0||x==x1||y==y0||y==y1)&&y!=(y0+y1)/2&&x!=(x0+x1)/2)
                obstacle(x,y,TOWN+(x==x0||x==x1?47:45));
            else if(x>x0&&x<x1&&y>y0&&y<y1&&x%2==0&&y%2==0)prop(x,y,FARM+crop);
        }
    }
    private void grove() {
        for(int x=0;x<WIDTH;x++)for(int y=0;y<HEIGHT;y++)
            if(!isFloor(x,y)&&props[x][y]==0&&Math.floorMod(x*17+y*13,3)!=0)
                prop(x,y,TOWN+(x%3==0?(level.number()==2?28:4):5));
    }
    private void obstacle(int x,int y,int tile) {floor[x][y]=0;prop(x,y,tile);}
    private void building(int left,int top,int tile,int w,int h) {
        for(int dx=0;dx<w;dx++)for(int dy=0;dy<h;dy++)obstacle(left+dx,top-dy,tile+dx+dy*12);
    }
    public boolean outdoors() {return level.number()>=2&&level.number()<=4;}
    public float startX() {return startTileX*TILE+8;}
    public float startY() {return startTileY*TILE+8;}
    public float exitX() {return exitTileX*TILE+8;}
    public float exitY() {return exitTileY*TILE+8;}

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
        if(level.theme()==CampaignLevel.Theme.SNOW)return SKI+(Math.floorMod(x*17+y*29,19)==0?1:0);
        if(level.theme()==CampaignLevel.Theme.VILLAGE)
            return ground[x][y]!=0?ground[x][y]:TOWN+(Math.floorMod(x*29+y*17,13)==0?1:0);
        if(level.theme()==CampaignLevel.Theme.FARM)
            return TOWN+((x>=12&&x<=23||x>=30&&x<=41)&&(y>=5&&y<=16||y>=21&&y<=30)?25:0);
        int hash=Math.floorMod(x*37+y*61+x*y*11,73);
        return hash==0?24:hash==1?12:0;
    }
    public int wallTile(int x,int y) {
        if(outdoors())return level.theme()==CampaignLevel.Theme.SNOW?SKI:TOWN;
        for(int[] torch:torches) if(torch[0]==x&&torch[1]==y) return torch[2]==0?29:32;
        if(isFloor(x,y-1)) {
            if(Math.floorMod(x*7+y,13)==0)return 19;
            if(y>18&&Math.floorMod(x*3+y,9)==0)return 20;
            return (x%4==0)?28:40;
        }
        if(isFloor(x-1,y))return 58;
        if(isFloor(x+1,y))return 57;
        return 40;
    }
    public boolean overgrown(int x,int y) { return level.number()==1&&x>=3&&x<=26&&y>=18&&y<=31; }
    public boolean carpet(int x,int y) {
        if(level.number()==5)return x>=36&&x<=48&&y>=27&&y<=29;
        if(level.number()!=1)return false;
        return (x>=8&&x<=11&&y>=6&&y<=9)||(x>=43&&x<=48&&y>=24&&y<=28);
    }
    /** Decorations never alter walkability; existing saves and corridor widths remain valid. */
    public int detailAt(int x,int y) {
        if(outdoors())return -1;
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
