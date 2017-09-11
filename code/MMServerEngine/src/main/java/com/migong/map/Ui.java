package com.migong.map;

/*
 * 迷宫算法的图形实现（郑玉振 2011\12\25）
 * 再看该功能之前请务必先学习完前面所有的功能
 *
 * 从CreateMao中获取随机地图，随机的拆除若干墙，使其变成多路径迷宫，再用该地图建立GetRoadByDeep和GetShortRoad实例，
 * 然后调用相应的getRoad函数获取深度遍历的路径数组和最短路径数组
 * 运用java的Graphics类在一个JFrame上绘制地图和相应的路径，其中深度遍历的路径用红色表示，最短路径用绿色表示，并标出所有结点的权值
 * 还有一个随机产生地图的按钮用于换地图，这将重绘整个地图和一个关闭按钮
 */

        import java.awt.*;
        import java.awt.event.*;
        import javax.swing.*;

public class Ui extends Frame implements ActionListener{

    JButton btn1,btn2;												//更改地图和关闭按钮
    int x = 20,y = 60;														//初始坐标点
    int nodeX = 16,nodeY = 16;												//node的大小
    CreateMap myMap=null;
    byte[][] map=null;												//存储迷宫地图的数组
    Element[] roadDeep=null;										//深度遍历的路径数组
    Element[] roadShort=null;										//最短路径数组
    int[][] roadLength=null;										//结点权值
    /**构造函数，接受四个参数，分别为：迷宫地图的行数，列数、入口，终点**/
    public Ui(int tr,int td,Element in,Element out){
        setLayout(new FlowLayout());								//流布局，默认就是此，可省略
        btn1=new JButton("更改地图");
        btn2=new JButton("关闭");
        add(btn1);
        add(btn2);
        btn1.addActionListener(this);								//注册监听器，Ui本身
        btn2.addActionListener(this);
        init(tr,td,in,out);											//初始化
        setVisible(true);											//使窗口可见
    }
    /**初始化相应的的参数**/
    private void init(int tr,int td,Element in,Element out){
        Dimension   screensize   =   Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)screensize.getWidth();
        int height = (int)screensize.getHeight();

//		System.out.println("width:"+width+",height:"+height);
//		x=20;														//地图离窗口左边的距离
//		y=60;														//地图离窗口上边的距离
        nodeX=nodeY = (height-140)/tr;													//单个结点的长
//		nodeY=16;													//单个节点的宽
        setSize(td*nodeX+50,tr*nodeY+140);							//设置窗口的大小
        System.out.println("sizeHeight:"+(tr*nodeY+140)+",useHeight:"+(height-y-30)/tr*tr);
        myMap=new CreateMap(tr,td,in,out);							//随机产生地图
        map=myMap.getMap();											//获取地图数组
        int num=((tr-1)*(td-1)/8)>6?6:((tr-1)*(td-1)/8);			//拆墙，最多拆六个
        for(int i=0;i<num;i++){
            int x=(int)(Math.random()*(tr-2))+1;					//1---tr-1
            int y=(int)(Math.random()*(td-2))+1;					//1---td-1
            map[x][y]=0;
        }
//		roadDeep=new GetRoadByDeep(myMap).getRoad();				//获取深度遍历的路径数组
//		GetShortRoad shortRoad=new GetShortRoad(myMap,false);
//		roadShort=new GetShortRoad(myMap,false).getRoad();			//获取最短路径数组
//		roadLength=shortRoad.getRoadLength();						//获取结点权值
    }
    /**绘图函数，系统自动调用的**/
    @Override
    public void paint(Graphics g){
        super.paint(g);
        g.setColor(Color.red);										//红色的墙
        int tr=map.length;											//行数和列数
        int td=map[0].length;
        Element in=myMap.getIn();									//入口和出口
        Element out=myMap.getOut();
        for(int i=0;i<tr;i++)										//绘制墙
            for(int j=0;j<td;j++){
                if((map[i][j]&8)==8)								//注意这里的为运算的用法
                    g.drawLine(x+j*nodeX, y+i*nodeY, x+(j+1)*nodeX, y+i*nodeY);
                if((map[i][j]&4)==4)
                    g.drawLine(x+j*nodeX, y+i*nodeY, x+j*nodeX, y+(i+1)*nodeY);
                if((map[i][j]&2)==2)
                    g.drawLine(x+j*nodeX, y+(i+1)*nodeY, x+(j+1)*nodeX, y+(i+1)*nodeY);
                if((map[i][j]&1)==1)
                    g.drawLine(x+(j+1)*nodeX, y+i*nodeY, x+(j+1)*nodeX, y+(i+1)*nodeY);
            }
//		g.setColor(new Color(255,0,0,100));							//DeepRoad，红色，半透明
//		for(int i=0;i<roadDeep.length;i++){
//			g.fillOval(x+roadDeep[i].y*nodeX+nodeX/2,y+roadDeep[i].x*nodeY/2,nodeX/4,nodeX/4);//行数对应列坐标
//		}
//		g.setColor(new Color(0,255,0,100));							//RoadShort，绿色，半透明
//		for(int i=0;i<roadShort.length;i++){
//			g.fillOval(x+roadShort[i].y*nodeX+nodeX/2,y+roadShort[i].x*nodeY+nodeY/2,nodeX/4,nodeX/4);
//		}
//		g.setColor(Color.black);									//路径权值
////		for(int i=0;i<roadLength.length;i++)
////			for(int j=0;j<roadLength[0].length;j++){
////				g.drawString(""+roadLength[i][j],x+j*nodeX+4,y+i*nodeY+12);
////			}
//		g.drawString("始", x+in.y*nodeX+2, y+in.x*nodeY+12);			//标记入口和终点
//		g.drawString("终", x+out.y*nodeX+2, y+out.x*nodeY+12);
//
//		g.drawString("红色为深度遍历路径（路径优先顺序：东，南，西，北）",10, tr*nodeY+100);
//		g.drawString("绿色为最短路径，深绿色为重合点",10, tr*nodeY+120);
    }
    /**监听函数，监听按钮动作，通过调用init函数重新创建新的地图，并重绘整个窗口**/
    public void actionPerformed(ActionEvent e){
        if(e.getActionCommand()=="更改地图"){
            init(myMap.getMap().length,myMap.getMap()[0].length,myMap.getIn(),myMap.getOut());
        }
        if(e.getActionCommand()=="关闭"){
            System.exit(0);
        }
        repaint();
    }
    /*主函数：测试函数（参数：地图的行和列，入口和出口）*/
    public static void main(String[] args) {
        int size = 380;
        new Ui(size,size,new Element(0,0),new Element(size-1,size-1));
    }
}
