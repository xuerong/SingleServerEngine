package com.migong.map;

/*
 * 0、1迷宫深度探索路径类 （郑玉振 2011/12/22）
 * 迷宫学习入手模块，存储较为简单，易于理解，可全注意力的去理解深度探索路径的原理
 *
 * 存储方式：1代表墙体，0代表通路，2代表终点（方便用于判断是否结束）
 * 			用一个数组存储地图，其中边墙除了入口和出口外，全为1，内部墙体也为1
 * 探索方式：从入口开始，将当前点入栈
 */
public class Main {
    //==入口为,0，出口为2，围墙为1，内部障碍为1，通路为0==
    int[][] maze={
            {1,1,1,1,1,1,1,1,1},
            {0,0,0,0,0,1,0,0,2},
            {1,0,1,0,0,1,0,1,1},
            {1,0,0,1,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1}
    };
    final int width=9,height=5;//迷宫长和宽
    Stack s1=new Stack(width*height);//s1用来存储已访问过的且处在当前路径上的节点
    Stack s2=new Stack(width*height);//s2存储在s1中具有为访问过的分支的节点
    private void getRoad(Element node){
        Element now = node.clone();//当前所处位置
        if(maze[now.x-1][now.y-1]==0){
            maze[now.x-1][now.y-1]=1;//设为“已访问”
        }
        int nodeNum=nextNodeAndNum(now);//now在这个地方被改为下一步了
        while(nodeNum==0){
            //如果S2为空，则“end”,否则，将S1出栈到S2中的最上面的哪一个，再将S2取出，计算其nextNum，如果为0，继续出栈，为1，则出S2，递归，如果大于1，递归。
            if(s2.isEmpty()){
                System.out.println("无路径，end");
                return;
            }else{
                Element s2Node=s2.pop(),s1Node;
                while(!s1.isEmpty()){
                    s1Node=s1.pop();
                    if(s1Node.equals(s2Node))
                        break;
                }
                node=s2Node;
                now=s2Node.clone();
                nodeNum=nextNodeAndNum(now);
            }
        }
        if(nodeNum==1){
            //入栈S1，递归
            s1.push(node);
            getRoad(now);
        }else{
            //入栈S1,S2，递归
            s1.push(node);
            s2.push(node);
            getRoad(now);
        }
    }
    private int nextNodeAndNum(Element node){
        int nodeNum=0;
        int x=0,y=0;//node的行和列的该变量
        //‘行和列’与‘数组’之间差一,防止越界
        if(node.x>1&&maze[node.x-2][node.y-1]==0){//北
            nodeNum++;
            x=-1;y=0;
        }else if(node.x>1&&maze[node.x-2][node.y-1]==2){
            //一个路径出来了
            writeRode(node,new Element(node.x-1,node.y));
        }
        if(node.y>1&&maze[node.x-1][node.y-2]==0){//西
            nodeNum++;
            x=0;y=-1;
        }else if(node.y>1&&maze[node.x-1][node.y-2]==2){
            writeRode(node,new Element(node.x,node.y-1));
        }
        if(node.x<height&&maze[node.x][node.y-1]==0){//南
            nodeNum++;
            x=1;y=0;
        }else if(node.x<height&&maze[node.x][node.y-1]==2){
            writeRode(node,new Element(node.x+1,node.y));
        }
        if(node.y<width&&maze[node.x-1][node.y]==0){//东
            nodeNum++;
            x=0;y=1;
        }else if(maze[node.x-1][node.y]==2){
            writeRode(node,new Element(node.x,node.y+1));
        }
        node.x+=x;node.y+=y;
        return nodeNum;
    }
    //传入的是，当前位置：处在终点旁边和终点
    private void writeRode(Element now,Element export){
        Stack s3=new Stack(30);
        Element a=null;
        while(!s1.isEmpty()){
            s3.push(s1.pop());
        }
        while(!s3.isEmpty()){
            a=s3.pop();
            System.out.println("("+a.x+","+a.y+")");
            s1.push(a);
        }
        System.out.println("("+now.x+","+now.y+")");
        System.out.println("("+export.x+","+export.y+")");
        System.exit(0);
    }
    public static void main(String[] arsc){
        Element a=new Element(2,1);//入口
        new Main().getRoad(a);
    }
}




















