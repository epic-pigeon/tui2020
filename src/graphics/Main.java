package graphics;

import graph.AdjacencyListDirectionalGraph;
import kotlin.Pair;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.*;

@SuppressWarnings("serial")
public class Main extends JFrame{
    // Define constants for the various dimensions
    public static final int CANVAS_WIDTH = 720;
    public static final int CANVAS_HEIGHT = 720;
    public static final Color LINE_COLOR = Color.BLACK;
    public static final Color CANVAS_BACKGROUND = Color.CYAN;

    // The moving line from (x1, y1) to (x2, y2), initially position at the center
    private int x1 = CANVAS_WIDTH / 2;
    private int y1 = CANVAS_HEIGHT / 8;
    private int x2 = x1;
    private int y2 = CANVAS_HEIGHT / 8 * 7;

    private DrawCanvas canvas; // The custom drawing canvas (an innder class extends JPanel)


    public AdjacencyListDirectionalGraph graph;
    public ArrayList<Pair<Integer, Integer>> graphCoords;

    public void initGraph(){
        graphCoords = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            graph.addVertex(i);
        }
        graph.addEdge(0, 1, 10);
        graph.addEdge(1, 2, 15);
        graph.addEdge(1, 3, 30);
        graph.addEdge(1, 4, 10);
        graph.addEdge(3, 4, 10);

        graphCoords.add(new Pair<>(500, 800));
        graphCoords.add(new Pair<>(400, 300));
        graphCoords.add(new Pair<>(700, 200));
        graphCoords.add(new Pair<>(100, 400));
        graphCoords.add(new Pair<>(200, 50));
    }

    // Constructor to set up the GUI components and event handlers
    public Main() {
        graph = new AdjacencyListDirectionalGraph();
        initGraph();

        // Set up a panel for the buttons
        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnLeft = new JButton("Move Left ");
        btnPanel.add(btnLeft);
        btnLeft.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                x1 -= 10;
                x2 -= 10;
                canvas.repaint();
                requestFocus(); // change the focus to JFrame to receive KeyEvent
            }
        });
        JButton btnRight = new JButton("Move Right");
        btnPanel.add(btnRight);
        btnRight.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                x1 += 10;
                x2 += 10;
                canvas.repaint();
                requestFocus(); // change the focus to JFrame to receive KeyEvent
            }
        });

        // Set up a custom drawing JPanel
        canvas = new DrawCanvas();
        canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

        // Add both panels to this JFrame's content-pane
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(canvas, BorderLayout.CENTER);
        cp.add(btnPanel, BorderLayout.SOUTH);

        // "super" JFrame fires KeyEvent
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                switch(evt.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        x1 -= 10;
                        x2 -= 10;
                        repaint();
                        break;
                    case KeyEvent.VK_RIGHT:
                        x1 += 10;
                        x2 += 10;
                        repaint();
                        break;
                }
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Handle the CLOSE button
        setTitle("Move a Line");
        pack();           // pack all the components in the JFrame
        setVisible(true); // show it
        requestFocus();   // set the focus to JFrame to receive KeyEvent
    }

    /**
     * Define inner class DrawCanvas, which is a JPanel used for custom drawing.
     */
    class DrawCanvas extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.getColor("F8F9FA"));
            //drawRoad(100, 100, 400, 300, 5, g);
            drawRoadNew(graph, graphCoords, 10, g);
        }

        public void drawRoad(int x1, int y1, int x2, int y2, double width, Graphics g){
            /*g.drawLine(x1, y1, x2, y2);
            Rectangle2D rect = new Rectangle2D.Double(x1-10, y1, 20, 40);
            AffineTransform transform = new AffineTransform();
            transform.rotate(Math.PI / 40);
            Shape rotatedRect = transform.createTransformedShape(rect);
            Graphics2D graphics = (Graphics2D) g;
            graphics.setColor(Color.blue);
            graphics.draw(rotatedRect);*/
            Polygon p = new Polygon();
            double hypotenuse = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)), cosA = (x2-x1)/hypotenuse, sinA = (y1-y2)/hypotenuse;
            /*try{
                double tgA = (y2 - y1) / (x2 - x1);
                cosA = 1/Math.sqrt(1 - tgA*tgA);
                sinA = tgA * cosA;
                System.out.println(tgA);
            }catch (ArithmeticException e){
                cosA = 0;
                sinA = 1;
            }*/
            p.addPoint((int)(x1-width*sinA), (int)(y1-width*cosA));
            p.addPoint((int)(x1+width*sinA), (int)(y1+width*cosA));
            p.addPoint((int)(x2+width*sinA), (int)(y2+width*cosA));
            p.addPoint((int)(x2-width*sinA), (int)(y2-width*cosA));
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(p);
            g2.setColor(Color.white);
            g2.fill(p);
        }

        public void drawRoadNew(AdjacencyListDirectionalGraph graph, ArrayList<Pair<Integer, Integer>> coords, int width, Graphics g){
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.white);
            for (int j: graph.getVertices()) {
                for (int i: graph.getNeighbors(j)) {
                    g2.setStroke(new BasicStroke(width));
                    g2.draw(new Line2D.Float(coords.get(i).component1(), coords.get(i).component2(), coords.get(j).component1(), coords.get(j).component2()));
                }
            }
        }
    }

    // The entry main() method
    public static void main(String[] args) {
        // Run GUI codes on the Event-Dispatcher Thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Main(); // Let the constructor do the job
            }
        });
    }
}
