package org.xendra.xendrian.server;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;

import javax.swing.JFrame;

public class Main extends Canvas {
        
        private int SCALE = 1;
        private int WIDTH = 800/SCALE, HEIGHT = 800/SCALE;
        private Dimension DIMENSION = new Dimension(WIDTH*SCALE, HEIGHT*SCALE);
        public BufferedImage image = new BufferedImage(WIDTH, WIDTH, BufferedImage.TYPE_INT_RGB);
        public int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        
        public Main() {
                JFrame frame = new JFrame();
                frame.setPreferredSize(DIMENSION);
                frame.add(this);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.setResizable(true);
                frame.pack();
                
                new Thread(() -> run(), "Main").start();
        }
        
        public static void main(String[] args) {
                new Main();
        }
        
        public void run() {
                while(true) {
                        render();
                        try {
                                Thread.sleep(16);
                        } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }
                }
        }
        
        public void render() {
                BufferStrategy buffer = getBufferStrategy();
                if(buffer == null) {
                        createBufferStrategy(3);
                        return;
                }
                Graphics g = buffer.getDrawGraphics();
                
                clear();
                
                int[][] p = new int[90][90];
                for(int y = 0; y < p.length; y++) {
                        for(int x = 0; x < p[y].length; x++) {
                                p[y][x] = new Random().nextInt(0x1000000);
                        }
                }
                hexGrid(8, p, 0, 0, 0.86);
                
                g.drawImage(image, 0, 0, 800, 800, null);
                g.dispose();
                buffer.show();
        }
        
        public void clear() {
                for(int i = 0; i < pixels.length; i++) {
                        pixels[i] = 0;
                }
        }
        
        public void hexGrid(double sidelength, int xsize, int ysize, int xOffset, int yOffset, double hexsize, int color) {
                double hexwidth = Math.sqrt(3)*sidelength;
                for(int y = 0; y < ysize; y++) {
                        int yr = (int) (1.5*y*sidelength)+yOffset;
                        for(int x = 0; x < xsize; x++) {
                                int xr = (int) (hexwidth*(x+0.5+(y%2==0 ? 0:0.5)))+xOffset;
                                
                                for(int y2 = (int) (-sidelength); y2 < sidelength; y2++) {
                                        int Y = yr + y2;
                                        for(int x2 = (int) (-hexwidth/2*hexsize); x2 < hexwidth/2*(hexsize); x2++) {
                                                int X = xr + x2;
                                                if(X < 0 || Y < 0 ) continue;
                                                if(Y >= image.getHeight()) {
                                                        ysize = y+1;
                                                        continue;
                                                }
                                                if(X >= image.getWidth()) {
                                                        xsize = x+1;
                                                        continue;
                                                }
                                                if(Math.abs(y2) <= Math.abs(sidelength-(Math.abs(x2)*sidelength/(hexwidth)))*hexsize) pixels[X+Y*image.getWidth()] = color;
                                                
                                        }
                                }
                        }
                }
        }

        public void hexGrid(double sidelength, int[][] pixels, int xOffset, int yOffset, double hexsize) {
                double hexwidth = Math.sqrt(3)*sidelength;
                
                int yscan = pixels.length;
                int xscan = pixels[0].length;
                
                for(int y = 0; y < yscan; y++) {
                        int yr = (int) (1.5*y*sidelength)+yOffset;
                        for(int x = 0; x < xscan; x++) {
                                int xr = (int) (hexwidth*(x+0.5+(y%2==0 ? 0:0.5)))+xOffset;
                                
                                for(int y2 = (int) (-sidelength); y2 < sidelength; y2++) {
                                        int Y = yr + y2;
                                        for(int x2 = (int) (-hexwidth/2*hexsize); x2 < hexwidth/2*(hexsize); x2++) {
                                                int X = xr + x2;
                                                if(X < 0 || Y < 0 ) continue;
                                                if(Y >= image.getHeight()) {
                                                        yscan = y+1;
                                                        continue;
                                                }
                                                if(X >= image.getWidth()) {
                                                        xscan = x+1;
                                                        continue;
                                                }
                                                if(Math.abs(y2) <= Math.abs(sidelength-(Math.abs(x2)*sidelength/(hexwidth)))*hexsize) this.pixels[X+Y*image.getWidth()] = pixels[y][x];
                                                
                                        }
                                }
                        }
                }
        }
}
