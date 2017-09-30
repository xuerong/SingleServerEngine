package com.migong.map;

/**
 * Created by Administrator on 2017/9/14.
 */
public class Element {
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
    public int toInt(int size){
        return x*size+y;
    }
    /* 克隆函数（注意：直接讲一个Element付给另一个Element，传递的只是地址，而用该函数可以实现值传递）*/
    public Element clone(){										//克隆函数，实现值传递
        return new Element(this.x,this.y);
    }
    /*比较函数，该函数重写了基类Object的方法（注意：直接‘==’比较的是地址，重写该函数用于迷宫路径探索中的点比较很好用）*/
    public boolean equals(Element a){							//比较函数，实现值比较
        return a.x==this.x&&a.y==this.y;
    }

    public String toString(){
        return x+","+y;
    }
}
