package com.migong.map;


/*创建随机地图类（郑玉振	2011\12\23）
 *调用该类，反回一个地图对象；且该地图对象只有唯一的通路（可通过随机删除若干墙而得到相应的多通路地图，最短路径算法中有应用）
 *调用getMap函数，即可得到相应地图的数组表示;调用getIn和getOut函数分别可以获取入口和出口
 *
 * 用byte存储点的类型，不同的位代表不同的墙，从右到左依次为右下左上（东南西北）这样，这样有利于计算，根据排列组合：
 * 无墙：0
 * 有一个墙：右（1）、下（2）、左（4）、上（8）
 * 有两个墙：右下（3）、右左（5）、右上（9）、下左（6）、下上（10）、左上（12）
 * 有三个墙：右下左（7）、右下上（11）、右左上（13）、下左上（14）
 * 有四个墙：15
 *
 * 除全地图最上和最左元素之外，其余都最多两个墙，分别为右墙和下墙
 * 初始时，有四种墙：15（左上角）、11（上）、7（左）、3（其他）
 * 其中，很多组合不会出现
 *
 * 生成方式：利用深度递归。即，设起始node为当前node，随机选取其周围的一个未访问过的node，然后再将该node设为当前node，进入循环。
 * 如果当前node周围没有满足条件的node，如果还有为访问过的node（即已访问node数量小于总node数量），则从已经访问过的node随机中
 * 选出一个，作为当前node，进入循环。注意：这里必须从已经访问过的node中选，否则，会出现两个不相通的路径空间
 */

        import java.util.ArrayList;
        import java.util.List;
        import java.util.Random;

public class CreateMap {
    private final int tr,td;										//列数和行数
    private byte[][] map=null;										//地图
    private boolean[][] visited=null;								//每个node是否访问过，初始时都为false
    private List<Element> visitedElement = new ArrayList<Element>();
    private int visitedNum;											//已经访问过的点的数量
    private Element in=null,out=null;								//出发点和终点
    /*
     * 唯一的构造函数，主要功能：接收参数、为变量赋初值、调用初始化函数，启动构造程序
     * 传入的参数依次为：行数（tr）、列数（td）、出发点（in）、终点（out）
     */
    public CreateMap(int tr,int td,Element in,Element out){
        this.tr=tr;
        this.td=td;
        this.in=in;
        this.out=out;
        visitedNum=0;
        map=new byte[tr][td];										//用参数决定图形大小
        visited=new boolean[tr][td];								//用参数决定访问标志大小
        init(map);													//初始化，包括map和visited
        create3(in);													//构造地图
        addWalls(tr,td);
//        showMap();
    }

    /***
     * 添加上面的墙和左边的墙
     */
    public void addWalls(int tr,int td){
        byte[][] newMap = new byte[tr+1][td+1];
        for(int i=0;i<tr+1;i++){
            for(int j=0;j<td+1;j++){
                if(i == 0 && j == 0){
                    newMap[i][j] = 0;
                }else if(i == 0){
                    newMap[i][j] = 2;
                }else if(j == 0){
                    newMap[i][j] = 1;
                }else{
                    newMap[i][j] = map[i-1][j-1];
                }
            }
        }
        map = newMap;
    }

    public boolean checkRouteWithoutSkill(List<Integer> routes){
        return checkRouteWithoutSkill(routes,in,out);
    }
    /**
     * 给一个路径是否通
     * 如果后面有技能，这个判断就可能出问题，要添加
     */
    public boolean checkRouteWithoutSkill(List<Integer> routes,Element in,Element out){
        if(routes == null || routes.size() < 1){
            return false;
        }
        int td = this.td+1;
        //
        int lastX = routes.get(0)/td,lastY = routes.get(0)%td;
        // 判断首
        if(lastX != in.x || lastY != in.y){
            return false;
        }
        //
        boolean first = true;
        for(int step : routes){
            if(first){
                first = false;
                continue;
            }
            int x = step/td;
            int y = step%td;
            // 相邻
            if(Math.abs(x - lastX) + Math.abs(y - lastY) != 1){
                return false;
            }
            // 通路
            if(!checkDoor(lastX,lastY,x,y)){
                return false;
            }
            lastX = x;
            lastY = y;
        }
        if(lastX != out.x || lastY != out.y){
            return false;
        }
        return true;
    }
    private boolean checkDoor(int lastX,int lastY,int x,int y){
        byte last = map[lastX][lastY];
        byte cur = map[x][y];
        // 左走
        if(lastX == x && lastY - y == 1){
            return (cur & 1) == 0;
        }
        // 右走
        if(lastX == x && lastY - y == -1){
            return (last & 1) == 0;
        }
        // 上
        if(lastX - x == 1 && lastY == y){
            return (cur & 2) == 0;
        }
        // 下
        if(lastX - x == -1 && lastY == y){
            return (last & 2) == 0;
        }
        return false;
    }
    /*
     * 初始化函数，主要功能：初始化地图、初始化访问情况（都为false）
     * 地突出视情况：左上角为（东西南北15），上面一排为（东南北11），左边一排为（东西南7）、其余为（东南3）
     */
    private void init(byte[][] map){
        map[0][0]=15;												//左上角
        for(int i=1;i<td;i++){										//上面一排（注意：从(0,1)开始）
            map[0][i]=11;
        }
        for(int i=1;i<tr;i++){										//左面一排（注意：从(1,0)开始）
            map[i][0]=7;
        }
        for(int i=1;i<tr;i++)										//其他（注意：从(1,1)开始）
            for(int j=1;j<td;j++)
                map[i][j]=3;
        for(int i=0;i<tr;i++)
            for(int j=0;j<td;j++)
                visited[i][j]=false;
    }
    /*
     * 构造地图函数（拆墙函数）。方法：深度遍历递归。
     * 步骤：
     * 1、获取参数作为当前node，设置为已访问，访问量加一
     * 2、判断其周围是否有可延伸的点（通过调用hasDoor函数）
     * 		如果有，随机选取一个（通过调用nextNode函数）作为参数，递归调用函数本身（create）
     * 		如果没有，判断是否还有为访问过的node
     * 			如果没有，够早结束
     * 			如果有，
     * 				1)、随机找一个node，
     * 				2）、如果未访问，从新找，直到找到已经访问的node
     * 				3）、将该node设置为未访问，访问数量减一
     * 				4）、将该node作为参数，递归调用函数本身（create）
     */
    int create2Count = 0;
    private void create(Element node){
        if(create2Count++%100 == 0){
            System.out.println("create2Count:"+create2Count+",visitedNum:"+visitedNum);
        }
        visited[node.x][node.y]=true;								//标志为已经访问
        visitedNum++;												//访问量加一
        if(hasDoor(node))
            create(nextNode(node,(int)(Math.random()*4)));			//用随机的相邻node递归调用
        else if(visitedNum<tr*td){
            Element a=new Element((int)(Math.random()*tr),(int)(Math.random()*td));
            while(!visited[a.x][a.y])								//获取已访问过的node
                a=new Element((int)(Math.random()*tr),(int)(Math.random()*td));
            visited[a.x][a.y]=false;
            visitedNum--;
            create(a);
        }
    }
    private void create3(Element node){
        while(visitedNum<tr*td){
            visited[node.x][node.y]=true;								//标志为已经访问
            visitedNum++;												//访问量加一
            int dir = chooseDir(node);
            if(dir>=0){
                node = nextNode(node,dir);
            }else if(visitedNum<tr*td){
                Element a=visitedElement.get((int)(Math.random()*visitedElement.size()));
                if(!visited[a.x][a.y]){								//获取已访问过的node{
                    throw new RuntimeException("not visited");
                }
                boolean hasDorr = hasDoor(a);
                while(!hasDorr){
                    visitedElement.remove(a);
                    a=visitedElement.get((int)(Math.random()*visitedElement.size()));
                    if(!visited[a.x][a.y]){								//获取已访问过的node{
                        throw new RuntimeException("not visited");
                    }
                    hasDorr = hasDoor(a);
                }
                visited[a.x][a.y]=false;
                visitedNum--;
                node = a;
            }
        }
    }

    private void create2(Element node){
        if(create2Count++%100 == 0){
            System.out.println("create2Count:"+create2Count+",visitedNum:"+visitedNum);
        }
        visited[node.x][node.y]=true;								//标志为已经访问
        visitedNum++;												//访问量加一
        int dir = chooseDir(node);
        if(dir>=0){
            create2(nextNode(node,dir));			//用随机的相邻node递归调用
        }else if(visitedNum<tr*td){
            Element a=visitedElement.get((int)(Math.random()*visitedElement.size()));
            if(!visited[a.x][a.y]){								//获取已访问过的node{
                throw new RuntimeException("not visited");
            }
            boolean hasDorr = hasDoor(a);
            while(!hasDorr){
                visitedElement.remove(a);
                a=visitedElement.get((int)(Math.random()*visitedElement.size()));
                if(!visited[a.x][a.y]){								//获取已访问过的node{
                    throw new RuntimeException("not visited");
                }
                hasDorr = hasDoor(a);
            }
            visited[a.x][a.y]=false;
            visitedNum--;
            create2(a);
        }
    }
    /*
     * 获取下一个node的函数：主要功能：根据传入的node和方向，获取相应下一个node
     * 传入的参数为：父node和方向
     * 步骤：
     * 1、判断方向
     * 2、根据方向和node的位置，判断相应方向的下一个node是否满足条件
     * 		如果是，返回该node，并释放上一个node的内存
     * 		如果否，随机产生一个非原方向上得方向，递归调用函数本身（由于该函数运行
     * 				之前就已经确定了该node有满足条件的相邻node，所以不必担心会出现死循环）
     */
    @SuppressWarnings({ "finally" })
    private Element nextNode(Element node,int direction){
        switch(direction){
            case 0:														//东墙
                if(node.y>=td-1||visited[node.x][node.y+1])				//最东边的或者已访问过的
                    return nextNode(node,(int)(Math.random()*3+1));		//1、2、3里面随机产生
                else{
                    map[node.x][node.y]&=14;							//拆掉东墙
                    Element result=new Element(node.x,node.y+1);		//创建新node
                    node=null;											//主动释放内存
                    return result;										//返回新node
                }
            case 1:														//南墙
                if(node.x>=tr-1||visited[node.x+1][node.y]){
                    int dir=(int)(Math.random()*3)+1;
                    if(dir==1)
                        dir=0;
                    return nextNode(node,dir);							//0、2、3里面随机产生
                }else{
                    map[node.x][node.y]&=13;
                    Element result=new Element(node.x+1,node.y);
                    node=null;
                    return result;
                }
            case 2:														//西墙
                if(node.y<=0||visited[node.x][node.y-1]){
                    int dir=(int)(Math.random()*3);
                    if(dir==2)
                        dir=3;
                    return nextNode(node,dir);							//0、1、3里面随机产生
                }else{
                    map[node.x][node.y-1]&=14;							//注意：node的西墙即为其西边node的东墙
                    Element result=new Element(node.x,node.y-1);
                    node=null;
                    return result;
                }
            case 3:														//北墙
                if(node.x<=0||visited[node.x-1][node.y])
                    return nextNode(node,(int)(Math.random()*3)+1);		//1、2、3中产生
                else{
                    map[node.x-1][node.y]&=13;							//注意：node的北墙即为其北边node的南墙
                    Element result=new Element(node.x-1,node.y);
                    node=null;
                    return result;
                }
            default:try{												//如果参数错误，抛出异常，返回null（一般不用，程序员才用）
                throw new Exception("方向错误，请确保在0-3之间");
            }catch(Exception e){}finally{return null;}
        }
    }
    /*
     * 函数功能：判断node四周是否有未走过的node，是则返回true，不是则false
     * 方法：如果node得一个方向上无node或有，但已经访问，则相应方向为false，将四周的false相或，即可得到结果
     */
    private boolean hasDoor(Element node){
        boolean east=false,west=false,south=false,north=false;		//初始化东西南北为false，用来标识是否越界
        if(node.x>0) north=true;									//如果北不越界，则north为true,下同
        if(node.x<tr-1)south=true;
        if(node.y>0)west=true;
        if(node.y<td-1)east=true;
        return 	(east?!visited[node.x][node.y+1]:false)||			//这里还存在的是否被访问过得判断
                (south?!visited[node.x+1][node.y]:false)||
                (west?!visited[node.x][node.y-1]:false)||
                (north?!visited[node.x-1][node.y]:false);
    }
    private int chooseDir(Element node){
        int[] visitedDir = new int[4];
        int count = 0;
        if(node.y<td-1 && !visited[node.x][node.y+1]){
            visitedDir[count++] = 0;
        }
        if(node.x<tr-1 && !visited[node.x+1][node.y]){
            visitedDir[count++] = 1;
        }
        if(node.y>0 && !visited[node.x][node.y-1]){
            visitedDir[count++] = 2;
        }
        if(node.x>0 && !visited[node.x-1][node.y]){
            visitedDir[count++] = 3;
        }
        if(count == 0){
            return -1;
        }
        if(count > 1){
            visitedElement.add(node);
        }
        return visitedDir[(int)(Math.random()*count)];
    }
    /*
     * 显示地图，显示map的值
     */
    private void showMap(){
        for(int i=0;i<tr;i++){
            for(int j=0;j<td;j++)
                System.out.print("\t"+map[i][j]);
            System.out.println("");
        }
    }
    /*返回地图函数（即返回map数组）、入口和出口 */
    public byte[][] getMap(){
        return map;
    }
    public Element getIn(){
        return in;
    }
    public Element getOut(){
        return out;
    }
    /*测试函数*/
    public static void main(String[] args) {
        new CreateMap(4,4,new Element(0,0),new Element(2,3)).showMap();		//== 大小，入口，出口
    }

}
