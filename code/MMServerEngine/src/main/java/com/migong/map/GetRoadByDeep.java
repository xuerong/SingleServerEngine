package com.migong.map;

/*
 * 迷宫深度优先探索路径类（郑玉振 2011\12\23）
 * 在学习该类之前请务必学习 Map01和CreateMap
 * 本功能的算法原理和“01迷宫深度探索路径类”一样，实现方式不同，他是对“CreateMap”创建的地图进行寻路径，且更好的利用了栈和递归算法
 *
 * 探索方式：和Map01差不多，不同点主要体现在四个方面：
 * 		1、用一个布尔类型的二维数组存储访问情况
 * 		2、双栈法改为单栈法
 * 		3、在循环函数开始判断是否到终点
 * 		4、nextNodeAndNum(Element node)的实现方式有所改善
 *
 * 其实，存在很多需要验证的地方，如，如果构造函数中的参数的null验证，这里就不考虑那么多了，那会增加很多对说明算法实现帮助不大的代码，
 * 但在编写应用程序时切勿如此！
 */

public class GetRoadByDeep {
    private byte[][] map=null;											//地图数组
    private Element in=null;											//入口
    private Element out=null;											//出口
    private boolean[][] visited=null;									//记录是否已访问
    private int tr=0,td=0;												//地图的行数和列数
    private Stack s1=null;												//s1用来存储已访问过的且处在当前路径上的节点
    private Element[] road=null;										//最终路径
    /*
     * 构造函数：接受参数CreateMap类型的参数，即一个随机迷宫地图
     * 功能： 1、初始化类变量并给类变量赋值
     * 		 2、将已访问显性的初始化问false
     * 		 3、用入口结点作参数调用获取路径函数（getRoad）
     */
    public GetRoadByDeep(CreateMap createMap){
        map=createMap.getMap();											//初始化map
        in=createMap.getIn();											//初始化入口结点
        out=createMap.getOut();											//出口结点
        tr=map.length;													//行数（如果地图是Null，将会出错，但省略了校验）
        td=map[0].length;												//列数
        s1=new Stack(tr*td);											//初始化栈（大小用他tr*td，有足够的栈大小，又不太大）
        visited=new boolean[tr][td];									//初始化已访问数组
        for(int i=0;i<tr;i++)
            for(int j=0;j<td;j++)
                visited[i][j]=false;
        getRoad(in);
    }
    /*获取路径函数，该函数只是对Map01类中的相应函数稍作修改，请参见Map01类中的getRoad函数*/
    private void getRoad(Element node){
        if(node.equals(out)){											//是否是终点
            writeRode(out);												//打印路径
            return;
        }
        Element now = node.clone();										//克隆当前所处位置
        visited[node.x][node.y]=true;									//设为“已访问”（其实可以）
        int nodeNum=nextNodeAndNum(now);								//now在这个地方被改为下一步了
        while(nodeNum==0){												//用while找到一个有相邻结点的node
            if(s1.isEmpty()){											//s1空了还没打印路径，说明没路径
                System.out.println("无路径，end");
                return;
            }else{
                node=s1.pop();											//把该结点从栈中取出
                now=node.clone();
                nodeNum=nextNodeAndNum(now);
            }
        }
        s1.push(node);													//结点压栈
        getRoad(now);													//以新节点为参数递归
    }
    /*
     * 获取结点可访问相邻结点数量和下一个结点函数
     * 与Map01中的相应函数相比：
     * 		1、这里针对位存储方式应用了位运算来判断是否是通路，尽量学会此技巧的应用
     * 		2、对边界结点的判断用了一种新的方法
     * 		3、将终点的判断和打印结果调到了getRoad函数中
     */
    private int nextNodeAndNum(Element node){
        boolean east=false,west=false,south=false,north=false;			//初始化东西南北为false，用来标识是否越界
        int nodeNum=0;													//可访问的相邻结点数量
        int addX=0,addY=0;												//下一个node在行和列上得增量
        if(node.x>0) north=true;										//如果北不越界，则north为true,下同
        if(node.x<tr-1)south=true;
        if(node.y>0)west=true;
        if(node.y<td-1)east=true;
        if(north?((map[node.x-1][node.y]&2)==2?false:(!visited[node.x-1][node.y])):false){//三目运算符?:的套欠，请耐心分析
            nodeNum++;
            addX=-1;addY=0;
        }
        if(west?((map[node.x][node.y-1]&1)==1?false:(!visited[node.x][node.y-1])):false){
            nodeNum++;
            addX=0;addY=-1;
        }
        if(south?((map[node.x][node.y]&2)==2?false:(!visited[node.x+1][node.y])):false){
            nodeNum++;
            addX=1;addY=0;
        }
        if(east?((map[node.x][node.y]&1)==1?false:(!visited[node.x][node.y+1])):false){
            nodeNum++;
            addX=0;addY=1;
        }
        node.x+=addX;node.y+=addY;
        return nodeNum;
    }
    /*打印路径结点，并将路径存入数组road，以终点为参数为什么和Map01种不同？*/
    private void writeRode(Element out){
        Element[] path=s1.getStack();							//取出栈中的路径
        road=new Element[path.length+1];
        for(int i=0;i<path.length;i++){
            System.out.println("("+path[i].x+","+path[i].y+")");
            road[i]=new Element(path[i].x,path[i].y);			//传值，且不可直接赋值传引用（road[i]=path[i]）
        }
        System.out.println("("+out.x+","+out.y+")");
        road[road.length-1]=new Element(out.x,out.y);
    }
    /*返回路径，在其他地方调用时用，这里将在图形实现中应用*/
    public Element[] getRoad(){
        return road;
    }
    /*主函数：测试函数（参数：地图的行和列，入口和出口）*/
    public static void main(String[] args) {
        new GetRoadByDeep(new CreateMap(6,6,new Element(0,0),new Element(5,5)));
    }
}

