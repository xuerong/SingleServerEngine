package com.migong.map;

/*
 * 点元素类和栈类（郑玉振	 2011\12\21）
 * 这两个类属于基本类，将会在其他功能和类中担任重要的角色，增强程序的模块化和可理解性，也方便程序的功能的扩展
 * 基本类编写一定要注意没一个细节（如传值还是引用，构造函数的设计等），稳固为重点，否则，将对整个应用层影响巨大
 */
/*
 * Element类：
 * 这是点元素类，主要用于存储一个迷宫位置及对位置的相关操作
 * 存储的方式是通过记录点的行和列（注意：这里是行和列，而不是横坐标和纵坐标，这样有利于数组存储多点的理解）
 */
class Element{
    public int x,y;												//行和列
    /*
     * 该类包括两个构造函数，无参构造和行列坐标作参数的构造函数，
     * 其中第二个有利于边构造边赋值（现实中很好用，建议留意该类构造函数的应用，编写java类库的大师们爱用的技巧）
     */
    public Element(){}											//无参构造函数
    public Element(int x,int y){								//行列参数构造函数，非常方便
        this.x=x;
        this.y=y;
    }
    /* 克隆函数（注意：直接讲一个Element付给另一个Element，传递的只是地址，而用该函数可以实现值传递）*/
    public Element clone(){										//克隆函数，实现值传递
        return new Element(this.x,this.y);
    }
    /*比较函数，该函数重写了基类Object的方法（注意：直接‘==’比较的是地址，重写该函数用于迷宫路径探索中的点比较很好用）*/
    public boolean equals(Element a){							//比较函数，实现值比较
        return a.x==this.x&&a.y==this.y;
    }
}
/*
 * Stack类
 * 栈类，用于构造一个栈，内部用的是数组存储，节约内存，增加存取速度
 * （注意：对于栈来说，无需插入或删除中间数据，相对于灵活的链表来说，用给定大小的数组存储可较大的提高其性能）
 */
class Stack{
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