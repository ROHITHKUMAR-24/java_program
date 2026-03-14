
import java.awt.*;
import java.awt.event.*;
class Calculator extends Frame implements ActionListener{
    TextField display;
    Button btn0,btn1,btn2,btn3,btn4,btn5,btn6,btn7,btn8,btn9,plus,minus,mul,div,equal,clear;

    Calculator() {
        display=new TextField("0");
        display.setBounds(50,50,400,50);
        btn0=new Button("0");
        btn0.setBounds(50,400,80,80);
        btn0.addActionListener(this);

        btn1=new Button("1");
        btn1.setBounds(50,300,80,80);
        btn1.addActionListener(this);

        btn2=new Button("2");
        btn2.setBounds(150,300,80,80);
        btn2.addActionListener(this);

        btn3=new Button("3");
        btn3.setBounds(250,300,80,80);
        btn3.addActionListener(this);

        btn4=new Button("4");
        btn4.setBounds(50,200,80,80);
        btn4.addActionListener(this);

        btn5=new Button("5");
        btn5.setBounds(150,200,80,80);
        btn5.addActionListener(this);

        btn6=new Button("6");
        btn6.setBounds(250,200,80,80);
        btn6.addActionListener(this);

        btn7=new Button("7");
        btn7.setBounds(50,100,80,80);
        btn7.addActionListener(this);

        btn8=new Button("8");
        btn8.setBounds(150,100,80,80);
        btn8.addActionListener(this);

        btn9=new Button("9");
        btn9.setBounds(250,100,80,80);
        btn9.addActionListener(this);

        plus=new Button("+");
        plus.setBounds(350,200,80,80);
        plus.addActionListener(this);
        minus=new Button("-");
        minus.setBounds(350,300,80,80);
        minus.addActionListener(this);
        mul=new Button("*");
        mul.setBounds(350,400,80,80);
        mul.addActionListener(this);
        div=new Button("/");
        div.setBounds(250,400,80,80);
        div.addActionListener(this);
        equal=new Button("=");
        equal.setBounds(150,400,80,80);
        equal.addActionListener(this);
        clear=new Button("Clear");
        clear.setBounds(350,100,80,80);
        clear.addActionListener(this);
        add(display);
        add(btn0);
        add(btn1);
        add(btn2);
        add(btn3);
        add(btn4);
        add(btn5);
        add(btn6);
        add(btn7);
        add(btn8);
        add(btn9);
        add(plus);
        add(minus);
        add(mul);
        add(div);
        add(equal);
        add(clear);
        setSize(500,600);
        setLayout(null);
        setTitle("calculator");
        setVisible(true);
    }
    public void actionPerformed(ActionEvent e){
        if(e.getSource()==btn0){
            String expre=display.getText();
            expre+='0';
            display.setText(expre);
        }
        if(e.getSource()==btn1){
            String expre=display.getText();
            expre+='1';
            display.setText(expre);
        }
        if(e.getSource()==btn2){
            String expre=display.getText();
            expre+='2';
            display.setText(expre);
        }
        if(e.getSource()==btn3){
            String expre=display.getText();
            expre+='3';
            display.setText(expre);
        }
        if(e.getSource()==btn4){
            String expre=display.getText();
            expre+='4';
            display.setText(expre);
        }
        if(e.getSource()==btn5){
            String expre=display.getText();
            expre+='5';
            display.setText(expre);
        }
        if(e.getSource()==btn6){
            String expre=display.getText();
            expre+='6';
            display.setText(expre);
        }
        if(e.getSource()==btn7){
            String expre=display.getText();
            expre+='7';
            display.setText(expre);
        }
        if(e.getSource()==btn8){
            String expre=display.getText();
            expre+='8';
            display.setText(expre);
        }
        if(e.getSource()==btn9){
            String expre=display.getText();
            expre+='9';
            display.setText(expre);
        }
        if(e.getSource()==plus){
            String expre=display.getText();
            expre+='+';
            display.setText(expre);
        }
        if(e.getSource()==minus){
            String expre=display.getText();
            expre+='-';
            display.setText(expre);
        }
        if(e.getSource()==mul){
            String expre=display.getText();
            expre+='*';
            display.setText(expre);
        }
        if(e.getSource()==div){
            String expre=display.getText();
            expre+='/';
            display.setText(expre);
        }
        if(e.getSource()==clear){
            display.setText("0");
        }
        if(e.getSource()==equal){
            int a,b,i;
            char oper='+';
            String expre=display.getText();
            String tempA="",tempB="";
            for( i=0;i<expre.length();i++){
                if(Character.isDigit(expre.charAt(i))){
                    tempA+=expre.charAt(i);
                    
                }
                else{
                    oper=expre.charAt(i);
                    break;

                }
            }
            tempB=expre.substring(i+1);
            a=Integer.parseInt(tempA);
            b=Integer.parseInt(tempB);
            int result=0;
            switch (oper) {
                case '+':
                    result=a+b;
                    break;
                case '-':
                    result=a-b;
                    break;
                case '*':
                    result=a*b;
                    break;
                case '/':
                    result=a/b;
                    break;

                default:
                    throw new AssertionError();
            }
            display.setText(String.valueOf(result));

        }
    }
    
    public static void main(String[] args) {
        new Calculator();
    }
}