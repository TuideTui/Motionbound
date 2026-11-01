import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Normalize the generated source to a deterministic, aligned 32px game grid. JDK only. */
public final class ImportHeroAtlas {
    public static void main(String[] args) throws Exception {
        BufferedImage source=ImageIO.read(new File("art-source/hero-actions-generated.png"));
        List<int[]> rows=bands(source,false);
        if(rows.size()!=4)throw new IllegalStateException("Expected four animation rows, got "+rows.size());
        BufferedImage target=new BufferedImage(256,128,BufferedImage.TYPE_INT_ARGB);
        double cell=source.getWidth()/8.0;
        for(int row=0;row<4;row++) {
            int top=rows.get(row)[0],bottom=rows.get(row)[1];
            int minX=source.getWidth(),maxX=0,idleTop=source.getHeight(),idleBottom=0;
            for(int y=top;y<=bottom;y++)for(int x=0;x<cell;x++)if(opaque(source,x,y)) {
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);idleTop=Math.min(idleTop,y);idleBottom=Math.max(idleBottom,y);
            }
            double scale=18.0/(idleBottom-idleTop+1);
            double pivot=(minX+maxX)/2.0;
            for(int col=0;col<8;col++)for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
                int sx=(int)Math.round(col*cell+pivot+(x-16)/scale);
                int sy=(int)Math.round(idleBottom+(y-24)/scale);
                if(sx<col*cell||sx>=(col+1)*cell||sy<top||sy>bottom||!opaque(source,sx,sy))continue;
                int rgb=source.getRGB(sx,sy);
                target.setRGB(col*32+x,row*32+y,0xff000000|(rgb&0xffffff));
            }
        }
        ImageIO.write(target,"png",new File("assets/characters/hero-actions.png"));
        // A diagnostic enlargement is outside assets and never packaged into the game.
        BufferedImage preview=new BufferedImage(1536,768,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<768;y++)for(int x=0;x<1536;x++)preview.setRGB(x,y,target.getRGB(x/6,y/6));
        File folder=new File("build/review");folder.mkdirs();
        ImageIO.write(preview,"png",new File(folder,"hero-actions-atlas.png"));
        System.out.println("Hero atlas: four directions, eight poses, fixed foot anchor, 256x128 RGBA");
    }
    private static boolean opaque(BufferedImage image,int x,int y) {
        return x>=0&&y>=0&&x<image.getWidth()&&y<image.getHeight()&&((image.getRGB(x,y)>>>24)&255)>=160;
    }
    private static List<int[]> bands(BufferedImage source,boolean columns) {
        List<int[]> bands=new ArrayList<>();int start=-1,last=-1;
        int limit=columns?source.getWidth():source.getHeight();
        for(int line=0;line<limit;line++) {
            int count=0;
            for(int cross=0;cross<(columns?source.getHeight():source.getWidth());cross++)
                if(opaque(source,columns?line:cross,columns?cross:line))count++;
            if(count>12){if(start<0)start=line;last=line;}
            else if(start>=0&&line-last>12){bands.add(new int[]{start,last});start=-1;}
        }
        if(start>=0)bands.add(new int[]{start,last});
        return bands;
    }
}
