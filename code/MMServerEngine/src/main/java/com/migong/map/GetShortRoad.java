package com.migong.map;

/*
 * 寻找最短路径类（郑玉振	 2011\12\24）
 * 这个是对GetRoadByDeep类的改进，可以寻找到最短路径，并且可以记录迷宫中每个点与入口点的最短距离
 * 用CreateMap自动生成的迷宫地图往往只有一条路径，所以这里用了一个算法，随机的从地图中去掉相应数量的墙，使地图通路变多
 *
 * 探索方式：从入口开始，将其可访问相邻路径的权值（距离入口点的路径长度）设置为当前点加一，并将该结点入栈，寻找下一个结点递归。
 * 这样，栈S1中将存储从入口到当前结点的路径
 * 当遇到终点时，将将S1中的路径复制到road中，（这一定是现已经探索过的路径中最短的一条，原因和结点权值的应用密切相关），
 * 然后退回上一个结点，进入递归循环。当遇到死路时，可退回上一个有可访问相邻结点的结点
 *
 * 和GetRoadByDeep相比，判断是否已访问的方式改为对权值大小的判断，即如果权值大于当前结点+1，即为可访问，否则，不可访问
 */

public class GetShortRoad {
    private byte[][] map=null;											//地图数组
    private Element in=null;											//入口
    private Element out=null;											//出口
    private int[][] roadLength=null;									//路径权值
    int tr=0,td=0;														//行数和列数
    Stack s1=null;														//s1用来存储已访问过的且处在当前路径上的节点
    Element[] road=null;												//时刻保持着已访问过路径中的最短路径
    boolean isFinish=false;												//是否访问完可从入口点不经过终点即可到达的所有结点
    /*
     * 构造函数：接受CreateMap类型的参数，即一个随机迷宫地图，和一个是否设置多路径的boolean值
     * 功能： 1、初始化类变量并给类变量赋值
     * 		 2、随机删除若干墙（最多把六个结点置0）
     * 		 3、将所有结点的权值初始化为一个较大的值（tr*td）
     * 		 4、用入口结点作参数调用获取路径函数（getRoad）
     */
    public GetShortRoad(CreateMap createMap,boolean multiplyRoad){
        map=createMap.getMap();											//获取地图数组
        in=createMap.getIn();											//入口
        out=createMap.getOut();											//终点
        tr=map.length;													//行和列
        td=map[0].length;
        if(multiplyRoad){												//随机删除若干墙，使单通路地图随机性的变成多通路地图
            int num=((tr-1)*(td-1)/8)>6?6:((tr-1)*(td-1)/8);
            for(int i=0;i<num;i++){
                int x=(int)(Math.random()*(tr-2))+1;					//1---tr-1（注意：边墙不能拆）
                int y=(int)(Math.random()*(td-2))+1;					//1---td-1
                map[x][y]=0;											//拆掉该结点所有的墙
            }
        }
        s1=new Stack(tr*td);											//初始化栈
        roadLength=new int[tr][td];										//结点权值
        for(int i=0;i<tr;i++)
            for(int j=0;j<td;j++)
                roadLength[i][j]=tr*td;									//初始化为一个足够大但不会过大的数
        roadLength[in.x][in.y]=0;										//入口点设置为0，因为入口点距离本身的距离为0
        getRoad(in);
    }
    /*
     * 获取路径函数（也是通过递归调用）
     * 该函数与GetRoadByDeep中getRoad函数的主要区别有：
     * 		1、在找到路径时将路径存入road
     * 		2、结束的条件由找到一条路径变为栈s1为空，并且所有的结点都被访问过
     * 		3、加入了对必须通过终点才可进入的节点的特殊访问，以确定他们的权值
     */
    private void getRoad(Element node){
        //如果已经访问完了所有的不需要通过终点方可访问的结点，此时，最短路径已经找到了剩下的工作是给需要通过终点方可访问的结点赋权值
        if(isFinish);
        else if(node.equals(out)){										//如果是终点
            s1.push(out);												//压栈终点，这样栈s1中即为完整的路径
            road=s1.getStack();											//获取最新路径
            s1.pop();													//终点处出栈
            writeRode(out);												//打印路径
            node=s1.pop();												//将终点之前的第一个点设置为当前结点，进行新的路径的探索
        }
        Element now = node.clone();
        int nodeNum=nextNodeAndNum(now);								//now在这个地方被改为下一步了
        while(nodeNum==0){
            if(s1.isEmpty()){
                if(!isFinish){											//访问必须通过终点才可到达的结点
                    isFinish=true;
                    getRoad(out);
                }
                return;
            }else{
                node=s1.pop();
                now=node.clone();
                nodeNum=nextNodeAndNum(now);
            }
        }
        s1.push(node);
        getRoad(now);													//递归调用
    }
    /*
     * 获取结点可访问相邻结点数量和下一个结点函数
     * 与GetRoadByDeep类中的中的nextNodeAndNum函数相比：
     * 		1、把是否已经访问的判断改为权值大小的比较
     * 		2、对边界结点的判断用了一种新的方法
     * 		3、将终点的判断和打印结果调到了getRoad函数中
     */
    private int nextNodeAndNum(Element node){
        boolean east=false,west=false,south=false,north=false;		//初始化东西南北为false，用来标识是否越界
        int nodeNum=0;
        int addX=0,addY=0;											//下一个node在行和列上得增量
        if(node.x>0) north=true;									//如果北不越界，则north为true,下同
        if(node.x<tr-1)south=true;
        if(node.y>0)west=true;
        if(node.y<td-1)east=true;
        if(east?((map[node.x][node.y]&1)==1?false:true):false){
            if(roadLength[node.x][node.y]+1<roadLength[node.x][node.y+1]){	//根据权值判断是否是可通的相邻结点（记住这里的+1）
                nodeNum++;
                addX=0;addY=1;
            }
        }
        if(south?((map[node.x][node.y]&2)==2?false:true):false){
            if(roadLength[node.x][node.y]+1<roadLength[node.x+1][node.y]){
                nodeNum++;
                addX=1;addY=0;
            }
        }
        if(west?((map[node.x][node.y-1]&1)==1?false:true):false){
            if(roadLength[node.x][node.y]+1<roadLength[node.x][node.y-1]){
                nodeNum++;
                addX=0;addY=-1;
            }
        }
        if(north?((map[node.x-1][node.y]&2)==2?false:true):false){
            if(roadLength[node.x][node.y]+1<roadLength[node.x-1][node.y]){
                nodeNum++;
                addX=-1;addY=0;
            }
        }
        if(addX!=0||addY!=0)
            roadLength[node.x+addX][node.y+addY]=roadLength[node.x][node.y]+1;
        node.x+=addX;node.y+=addY;
        return nodeNum;
    }
    private void writeRode(Element out){
        Element[] path=s1.getStack();									//取出栈中的路径
        for(int i=0;i<path.length;i++){
            System.out.println("("+path[i].x+","+path[i].y+")");
        }
        System.out.println("("+out.x+","+out.y+")----------"+(path.length+1));//显示路径长度
    }
    /******获取路径权值******/
    public int[][] getRoadLength(){
        return roadLength;
    }
    /***获取路径***/
    public Element[] getRoad(){
        return road;
    }
    /**测试函数**/
    public static void main(String[] args) {
        new GetShortRoad(new CreateMap(12,12,new Element(0,0),new Element(11,11)),true);//多路径测试
    }
}

