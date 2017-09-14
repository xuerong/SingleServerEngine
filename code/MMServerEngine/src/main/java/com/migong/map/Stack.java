package com.migong.map;

/**
 * Created by Administrator on 2017/9/14.
 */
public class Stack {
    private int num;											//栈中现有数据量
    private Element[] stack=null;								//栈的存储
    private int max;											//栈的大小
    /*
     * 构造函数：一个参数，即栈的大小，注意这里一定要分配足够的大小，但不宜太多
     * 初始化栈的大小，栈中数据量即根据栈的大小构造栈的存储
     */
    public Stack(int max){
        this.max=max;
        stack=new Element[max];
        num=0;
    }
    /*判断空函数：用于连续取值。当用于路径存储时，可用于判断路径情况*/
    public boolean isEmpty(){
        return num<1;
    }
    /*获取栈大小：存储路径时，可用于判断路径的长度*/
    public int getNum(){
        return num;
    }
    /*
     * 获取栈内容函数：这个函数主要是取出栈中的的值，存储路径时，用于一次性取出路径，弥补了栈的但数据存取的缺陷.
     * （注意：取出数据时，一定要取值，而不要是对战中内容的引用，否则，对取出值的操作将直接影响栈中内容，让人错都不知那错的）
     */
    public Element[] getStack(){
        Element[] result=new Element[num];						//要新构建一块数据用于返回值，切不可 return stack;
        for(int i=0;i<num;i++){
            result[i]=stack[i].clone();							//要构建新的Element用于返回值，切不可result[i]=stack[i];
        }
        return result;
    }
    /*
     * 存栈顶值函数：注意：1、栈中现有数据量从‘1’开始，而存值数组从‘0’开始
     * 			2、存执时，一定要存它的副本，即传值，切不可传引用，否则，对外部的值的操作将打乱栈中数据，错都不知道哪错的
     * 			3、栈中无值时抛出并处理异常（其实，在这里只抛出，不处理更能增强调用者控制的灵活性，但这样更方便）
     */
    public void push(Element node){
        if(num<max){											//当栈未满
            stack[num]=new Element(node.x,node.y);				//压栈时传值！
            num++;												//栈中数量加一
        }else{
            try {												//抛出并处理异常
                throw new Exception("栈溢出，请设置更大的栈！");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /*取栈顶函数：注意：这里取值可直接返回引用，因为当num减一时，栈就失去了对该值引用的意义，
     * 传值的话，由于原数据还被栈中数组引用，虚拟机并不释放它，我们却用不到它了，造成无意义的内存浪费
     */
    @SuppressWarnings("finally")
    public Element pop(){
        if(num>0){												//当栈中有值时
            num--;												//栈中数量减一
            return stack[num];									//返回的是引用
        }else{
            try {
                throw new Exception("栈已空！");
            } catch (Exception e) {
                e.printStackTrace();
            }finally{
                return null;
            }
        }
    }
}
