package com.migong.map;

/*
 * 01迷宫深度探索路径类 （郑玉振 2011/12/22）
 * 迷宫学习入手模块，存储较为简单，运用两个栈存储，降低对递归理解的难度，易于理解，可全注意力的去理解深度探索路径的原理
 * （GetRoadByDeep也是用此原理，但存储方式和递归运用和响应函数的实现代码更加难理解，但存储空间和运行机理更加优化，更实用）
 *
 * 存储方式：1代表墙体，0代表通路，2代表终点（方便用于判断是否结束）
 * 			用一个数组存储地图，其中边墙除了入口和出口外，全为1，内部墙体也为1
 * 探索方式：从入口开始，将所走路径存入栈s1，而栈s2用于存储路上所有具有未访问分支的结点。当进入新结点时，将相应位置置为1
 * 即，已访问，当遇到死路时，可从s2中取出一个值，进行访问。当遇到出口时，结束，s1中即为路径，若取s2时出现无值的现象，说明
 * 该迷宫无路径，返回
 */

public class Map01 {
    int[][] map={												//==入口为,0，出口为2，围墙为1，内部障碍为1，通路为0==
            {1,1,1,1,1,1,1,1,1},
            {0,0,0,0,0,1,0,0,1},
            {1,0,1,0,0,1,0,1,1},
            {1,0,0,1,0,0,0,0,2},
            {1,1,1,1,1,1,1,1,1}
    };
    final int width=9,height=5;									//迷宫长和宽（只读）
    Stack s1=new Stack(width*height);							//s1用来存储已访问过的且处在当前路径上的结点
    Stack s2=new Stack(width*height);							//s2存储在s1中具有为访问过的分支的结点
    /*
     * 获取路径函数：也为入口函数，通过简单的递归获取相应的路径，传入一个结点作为参数
     * 步骤：1、克隆当前位置，主要是后面将把克隆者改变为下一个结点
     * 		 2、若该点没访问，设置为已访问（0变1）
     * 		3、获取该点的可走的相邻点数目和下一个点（优先级：东、南、西、北）
     * 		4、1):如果没有可访问相邻点,若栈s2空，无路径，返回，否则，弹出一个点，并将栈s1中的结点弹出到该结点，
     * 				判断相邻结点状况，进入循环，直到弹出有可访问相邻结点的点
     * 			2)、如果有一个可访问的相邻结点，压栈s1，并将下一个结点作为参数，递归
     * 			3)、如果有两个可访问的相邻结点，压栈s1和s2，并将下一个结点作为参数，递归
     */
    private void getRoad(Element node){
        Element now = node.clone();								//克隆当前结点
        if(map[now.x][now.y]==0){
            map[now.x][now.y]=1;								//设为“已访问”
        }
        int nodeNum=nextNodeAndNum(now);						//注意：now在这个地方‘可能’被改为下一结点了
        while(nodeNum==0){										//直到找到一个具有可访问相邻路径的点，才跳出循环
            if(s2.isEmpty()){									//s2空，无路径，结束
                System.out.println("无路径，end");
                return;
            }else{
                Element s2Node=s2.pop(),s1Node;
                while(!s1.isEmpty()){							//使s1弹出结点到s2中的上结点，即开始新路径
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
            s1.push(node);										//压栈s1，递归
            getRoad(now);
        }else{
            s1.push(node);										//入栈S1,S2，递归
            s2.push(node);
            getRoad(now);
        }
    }
    /*
     * 相邻结点数量和下一结点函数：判断进入点所拥有的可访问相邻点的数量，若有，改变参数，使其变成下一个结点
     * 如果下一结点即为终点，则栈s1中的结点+当前结点+终点即为最终路径，并调用打印路径函数
     * 注意：包括：边上的结点相应方向是否越界判断 和 临界点是否访问或墙壁判断
     */
    private int nextNodeAndNum(Element node){
        int nodeNum=0;											//可访问相邻结点数
        int x=0,y=0;											//node的行和列的该变量，记录下一个结点与当前结点的差
        if(node.x>0&&map[node.x-1][node.y]==0){					//北
            nodeNum++;
            x=-1;y=0;
        }else if(node.x>1&&map[node.x-1][node.y]==2)			//判断是否结束
            writeRode(node,new Element(node.x,node.y+1));		//打印路径
        if(node.y>1&&map[node.x][node.y-1]==0){					//西
            nodeNum++;
            x=0;y=-1;
        }else if(node.y>1&&map[node.x][node.y-1]==2)
            writeRode(node,new Element(node.x,node.y-1));
        if(node.x<height&&map[node.x+1][node.y]==0){			//南
            nodeNum++;
            x=1;y=0;
        }else if(node.x<height&&map[node.x+1][node.y]==2)
            writeRode(node,new Element(node.x+1,node.y));
        if(node.y<width&&map[node.x][node.y+1]==0){				//东
            nodeNum++;
            x=0;y=1;
        }else if(map[node.x][node.y+1]==2)
            writeRode(node,new Element(node.x,node.y+1));
        node.x+=x;node.y+=y;									//下一给点（改变now，也可能没下一个点）
        return nodeNum;
    }
    /*打印路径函数，由于被调用时当前点和终点还未入栈，所以这两个点作为参数传进来的*/
    private void writeRode(Element now,Element export){			//参数当前点和终点
        Element[] path=s1.getStack();							//取出栈中的路径
        for(int i=0;i<path.length;i++)
            System.out.println("("+path[i].x+","+path[i].y+")");
        System.out.println("("+now.x+","+now.y+")");
        System.out.println("("+export.x+","+export.y+")");
        System.exit(0);											//退出程序
    }
    /*主函数：测试函数*/
    public static void main(String[] arsc){
        new Map01().getRoad(new Element(1,0));					//入口
    }
}
