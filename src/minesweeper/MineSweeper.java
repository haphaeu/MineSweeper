/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import static javax.swing.SwingUtilities.isLeftMouseButton;


/**
 *
 * @author raf
 */
public class MineSweeper implements KeyListener, MouseListener {
    
    MyDrawPanel panel;
    int xCursor=250, yCursor=300;
    int sBricks = 30;
    int gap = 5;
    int gap_footer = 50;
    boolean isGameOver = false;
    boolean isGameOverWin;
    
    static int cols = 20;
    static int rows = 20;
    int nBombs = 50;
    static final int BOMB = 99;
    
    // board will be:
    //   BOMB
    //   sum of neighbord bombs
    int[][] board = new int[rows][cols];
    
    // cells hidden are shown as grey brick.
    // unhidden cells are shown number
    boolean[][] hidden = new boolean[rows][cols];
    
    // right click - potential bomb locations
    boolean[][] maybeBomb = new boolean[rows][cols];
    
    Random rand = new Random();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        MineSweeper game = new MineSweeper(); 
        
        if (args.length > 0)
            game.nBombs = Integer.min(cols*rows, Integer.parseInt(args[0]));
        
        game.setup();
    }
    
    public void setup() {
        JFrame frame = new JFrame("Mine Sweeper");
        panel = new MyDrawPanel();
        frame.getContentPane().add(panel);
        frame.addKeyListener(this);
        frame.addMouseListener(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(cols*(sBricks+gap)+gap, rows*(sBricks+gap)+gap+gap_footer);
        frame.setResizable(false);
        frame.setVisible(true);
        initBricks();
    }
    
    int getNumBombs() {
        int n = 0;
        for(int r=0; r < rows; r++) 
                for(int c=0; c < cols; c++) 
                    if (board[r][c] == BOMB)
                        n += 1;
        return n;
    }
    
    int getNumMaybeBombs() {
        int n = 0;
        for(int r=0; r < rows; r++) 
                for(int c=0; c < cols; c++) 
                    if (maybeBomb[r][c])
                        n += 1;
        return n;
    }
    
    void initBricks() {
        
        // init zero boards
        for(int r=0; r < rows; r++) 
            for(int c=0; c < cols; c++) {
                board[r][c] = 0;
                hidden[r][c] = true;
                maybeBomb[r][c] = false;
            }
                
        // place random bombs in the board
        int r, c;
        int bombs = 0;
        while (bombs < nBombs) {
            r = (int) (rand.nextDouble() * rows);
            c = (int) (rand.nextDouble() * cols);
            board[r][c] = BOMB;
            bombs = getNumBombs();
        }
        
        // calc number of neighbor bombs
        for(r=0; r < rows; r++) 
            for(c=0; c < cols; c++) 
                if (board[r][c] != BOMB)
                    for(int cc = (c==0?0:c-1); cc <= (c==cols-1?c:c+1); cc++)
                        for(int rr = (r==0?0:r-1); rr <= (r==rows-1?r:r+1); rr++)
                            if (board[rr][cc] == BOMB)
                                board[r][c]++;
        
        for(r=0; r < rows; r++) {
            for(c=0; c < cols; c++) {
                System.out.printf(" %3d ", board[r][c]);
            }
            System.out.println("");
        }
        System.out.println("Board initialised.");
        
    }
    
    void checkClick(int row, int col, boolean isLeftClick) {
        
        // ignore left click on question marks
        if (maybeBomb[row][col] && isLeftClick)
            return;
        
        if (!isLeftClick) {
            
            // right click -> maybe bomb? put a question mark
            maybeBomb[row][col] = !maybeBomb[row][col];
            
        } else if (board[row][col] == BOMB) {
            
                // clicked on a bomb - dead...
                gameOver(false);
                
        } else {
            hidden[row][col] = false;

            if (board[row][col] == 0) 
            // clicked on a barrends field. expand all surounding zeros
            expandZeros(row, col);
        }
        
        // check current status for a win
        int sum = BOMB * nBombs;
        for(int r=0; r < rows; r++) 
            for(int c=0; c < cols; c++)
                if (maybeBomb[r][c])
                    sum -= board[r][c];
        if (sum == 0)
            gameOver(true);

    }
    
    @SuppressWarnings("empty-statement")
    void expandZeros(int row, int col) {
        // Expand countour region with zeros
        //
        // Bug: may miss islands of zeros in the middle
        //      due to lazy algorithm ;)
        //
        
        System.out.println("Called expend at " + row + " " + col);
        
        // implement a contour algorithm... 
        //   1. find and save border
        //   2. loop
        //   2.1 go left
        //   2.2 if back to start: done
        
        // find border by going left
        // start point will be (r0, c0)
        int r0 = row;
        int c0 = -9;
        for (int c = col; c >= 0 && board[row][c] == 0; c--) {
            c0 = c;
            hidden[r0][c0] = false;
        }
        
        // now, image you're inside the region, looking at the border
        // just go left
        //
        // there are four directions: left, down, right, up
        // 
        // if going in a direction jumps outside the region:
        //     try next direction
        // if stays in region:
        //     update position
        //     try previous direction
        //
        
        int[] moves_row = {0, 1, 0, -1};
        int[] moves_col = {-1, 0, 1, 0};
        int direction = 0;
        int r_try, c_try, ri = r0, ci = c0;
        while (true) {
            
            // try a move in a  direction
            r_try = ri + moves_row[direction];
            c_try = ci + moves_col[direction];
            
            // check if still inside region or not
            if (r_try >= 0 && r_try < rows && c_try >= 0 && c_try < cols) {
                
                hidden[r_try][c_try] = false;
                
                if (board[r_try][c_try] == 0) {
                    // success - still inside region
                    ri = r_try;
                    ci = c_try;
                    // previous direction
                    direction = (direction + 3) % 4; 

                    if (ri == r0 && ci == c0)
                        break;
                } else {
                    // out of region, try again in next direction
                    direction = (direction + 1) % 4;
                }
            } else {
                // out of region, try again in next direction
                direction = (direction + 1) % 4;
            }
        }
    }
    
    void gameOver(boolean win) {
        isGameOverWin = win;
        
        for(int r=0; r < rows; r++) 
            for(int c=0; c < cols; c++)
                hidden[r][c] = false;
        
        System.out.println("Game over");
        isGameOver = true;
        panel.repaint();
    }
    
    // KeyPressed interface
    @Override
    public void keyPressed(KeyEvent ev) {
        System.out.print("Key pressed...");
        switch (ev.getKeyCode()) {
            case KeyEvent.VK_R:
                // restart
                System.out.println("restart");
                isGameOver    = false;
                initBricks();
                panel.repaint();
                break;
            default:
                System.out.println("not implemented");
                break;
        }
    }
    @Override public void keyReleased(KeyEvent ev) {}
    @Override public void keyTyped(KeyEvent ev) {}
   
    // MouseInterface
    @Override public void mouseClicked​(MouseEvent e) {
        xCursor = e.getPoint().x; 
        yCursor = e.getPoint().y - 28;
        if (!isGameOver) {
            boolean isLeftClick = isLeftMouseButton(e);
            int row, col;
            col = (xCursor - gap) / (sBricks + gap);
            row = (yCursor - gap) / (sBricks + gap);
            System.out.println("Mouse clicked at brick row, col:" + row + ", " + col);
            if (col < cols && row < rows) {
                checkClick(row, col, isLeftClick);
                panel.repaint();
            }
            
        }
    }
    @Override public void mouseEntered​(MouseEvent e) {}
    @Override public void mouseExited​(MouseEvent e)  {}
    @Override public void mousePressed​(MouseEvent e)  {}
    @Override public void mouseReleased​(MouseEvent e) {}
            
    // drawings
    class MyDrawPanel extends JPanel {
        @Override
        public void paintComponent(Graphics gfx) {
            
            int w = cols * (sBricks + gap);
            int h = rows * (sBricks + gap);
            String tmp;
            
            gfx.setColor(Color.black);
            gfx.fillRect(0, 0, w, h+gap_footer);

            gfx.setColor(Color.orange);
            gfx.drawString("Remaining: " + (nBombs - getNumMaybeBombs()) + " bombs", 0, h+15);

            // draw grid
            for(int r=0; r < rows; r++) {
                gfx.setColor(Color.gray);
                gfx.drawLine(0, r*(sBricks+gap), w, r*(sBricks+gap));
                for(int c=0; c < cols; c++) {
                    
                    gfx.setColor(Color.gray);
                    gfx.drawLine(c*(sBricks+gap), 0, c*(sBricks+gap), h);
                    tmp = "";
                    if (hidden[r][c]) {
                        gfx.setColor(Color.gray);
                        gfx.fillRect(c*(sBricks+gap)+gap/2, r*(sBricks+gap)+gap/2, sBricks, sBricks);
                        if (maybeBomb[r][c]) {
                            gfx.setColor(Color.orange);
                            tmp = "?";
                        }
                    } else {
                        gfx.setColor(Color.gray);
                        
                        switch (board[r][c]) {
                            case 0:
                                tmp = "";
                                break;
                            case BOMB:
                                if (isGameOverWin) {
                                    gfx.setColor(Color.green);
                                    tmp = "V";
                                } else {
                                    gfx.setColor(Color.red);
                                    tmp = "X";
                                }
                                break;
                            default:
                                tmp = "" + board[r][c];
                                break;
                        }
                    }
                    gfx.drawString(tmp, 
                                   (int)((c+0.5)*(sBricks+gap)), 
                                   (int)((r+0.75)*(sBricks+gap)));
                }
            }
            
            if (isGameOver) {
                gfx.setColor(Color.black);
                gfx.fillRect(0, h, w, gap_footer);
                
                if (isGameOverWin)
                    gfx.setColor(Color.green);
                else
                    gfx.setColor(Color.red);
                
                gfx.drawString("Game over", 0, h+10);
                gfx.drawString("Press R to restart.", 0, h+20);
            }
        }
    }
    
}
