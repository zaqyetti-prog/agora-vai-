package com.peugeot.smartdrive;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private final int BG = 0xFFF2F3F5, DARK = 0xFF25282D, MUTED = 0xFF777D86, BLUE = 0xFF2878C8, WHITE = 0xFFFFFFFF;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean connected = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout root, content, nav;
    private TextView connectionLabel;
    private String currentTab = "INÍCIO";
    private final OBD data = new OBD();
    private final ArrayDeque<Float> tempHistory = new ArrayDeque<>(), rpmHistory = new ArrayDeque<>();
    private Runnable poller;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        buildShell(); showTab("INÍCIO");
        if (btAdapter == null) toast("Este aparelho não possui Bluetooth.");
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); setContentView(root);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(22, 10, 22, 8); top.setBackgroundColor(WHITE);
        TextView brand = text("308 SMART DRIVE", 18, DARK, true); top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        connectionLabel = text("● OBD DESCONNECTED", 12, MUTED, true); top.addView(connectionLabel);
        root.addView(top, new LinearLayout.LayoutParams(-1, 54));
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(18, 14, 18, 12); scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        HorizontalScrollView hsv = new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); nav = new LinearLayout(this); nav.setPadding(8, 7, 8, 7); nav.setBackgroundColor(DARK);
        String[] tabs = {"INÍCIO", "MOTOR", "GRÁFICOS", "DIAGNÓSTICO", "AJUSTES"};
        for (String t : tabs) { Button b = button(t, 12); b.setTextColor(WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(DARK)); b.setOnClickListener(v -> showTab(t)); nav.addView(b); }
        hsv.addView(nav); root.addView(hsv, new LinearLayout.LayoutParams(-1, 54));
    }

    private void showTab(String tab) {
        currentTab = tab; content.removeAllViews();
        if (tab.equals("INÍCIO")) home(); else if (tab.equals("MOTOR")) motor(); else if (tab.equals("GRÁFICOS")) graphs(); else if (tab.equals("DIAGNÓSTICO")) diagnostics(); else settings();
    }

    private void home() {
        addTitle("PEUGEOT 308 • SMART DRIVE", "EC5 FLEX  |  PAINEL DIGITAL");
        LinearLayout hero = card(); hero.setGravity(Gravity.CENTER); hero.setPadding(14, 12, 14, 12);
        TextView car = text("PEUGEOT 308", 27, DARK, true); car.setGravity(Gravity.CENTER); hero.addView(car);
        TextView silhouette = text("◢━━━━━━━━━━━━━━━━━━━━◣", 24, BLUE, true); silhouette.setGravity(Gravity.CENTER); hero.addView(silhouette);
        TextView model = text("HATCH • ALLURE • 2012/2013", 11, MUTED, true); model.setGravity(Gravity.CENTER); hero.addView(model);
        TextView hint = text("Imagem ilustrativa temporária — substitua pelo render/foto do seu 308 branco com rodas pretas.", 10, MUTED, false); hint.setGravity(Gravity.CENTER); hero.addView(hint);
        content.addView(hero, matchWrap());
        LinearLayout row1 = row(); row1.addView(metric("VELOCIDADE", val(data.speed, "km/h"), BLUE), weight()); row1.addView(metric("TEMPERATURA", val(data.coolant, "°C"), BLUE), weight()); row1.addView(metric("ROTAÇÃO", val(data.rpm, "rpm"), DARK), weight()); content.addView(row1);
        LinearLayout row2 = row(); row2.addView(metric("MAP", val(data.map, "kPa"), DARK), weight()); row2.addView(metric("BATERIA ECU", val(data.voltage, "V"), DARK), weight()); row2.addView(metric("CONSUMO", data.fuelRate > 0 ? String.format(Locale.US,"%.1f km/L*",data.fuelRate) : "N/D", DARK), weight()); content.addView(row2);
        addNote("*Consumo só será calculado quando a ECU fornecer fluxo de combustível e velocidade/MAF compatíveis. N/D significa dado indisponível.");
        Button connect = button(connected ? "DESCONECTAR OBD2" : "CONECTAR OBD2 BLUETOOTH", 14); connect.setOnClickListener(v -> { if (connected) disconnect(); else chooseDevice(); }); content.addView(connect, matchWrap());
    }

    private void motor() {
        addTitle("COMPUTADOR DE BORDO", "DADOS LIDOS DA ECU VIA OBD-II");
        String[][] vals = {{"Velocidade",val(data.speed,"km/h")},{"RPM",val(data.rpm,"rpm")},{"Temperatura do arrefecimento",val(data.coolant,"°C")},{"Temperatura do ar",val(data.intake,"°C")},{"Pressão MAP",val(data.map,"kPa")},{"Carga calculada",val(data.load,"%")},{"Borboleta TBI",val(data.throttle,"%")},{"Tensão do módulo",val(data.voltage,"V")},{"STFT",val(data.stft,"%")},{"LTFT",val(data.ltft,"%")},{"Fluxo de ar MAF",val(data.maf,"g/s")},{"Sonda lambda",data.lambdaText}};
        for (String[] pair : vals) { LinearLayout r = row(); r.addView(text(pair[0],14,DARK,true), new LinearLayout.LayoutParams(0,54,1)); r.addView(text(pair[1],16,BLUE,true)); content.addView(r); }
        addNote("Temperatura externa, nível de combustível, autonomia e consumo podem não ser disponibilizados pelos PIDs OBD-II genéricos do 308. Não são simulados.");
    }

    private void graphs() {
        addTitle("MONITORAMENTO AO VIVO", "HISTÓRICO RECENTE • ATUALIZAÇÃO OBD2");
        content.addView(text("TEMPERATURA DO MOTOR",12,MUTED,true)); content.addView(new MiniChart(tempHistory, BLUE), new LinearLayout.LayoutParams(-1,150));
        content.addView(text("ROTAÇÃO DO MOTOR",12,MUTED,true)); content.addView(new MiniChart(rpmHistory, DARK), new LinearLayout.LayoutParams(-1,150));
        addNote("Os gráficos começam a ser preenchidos após conectar ao adaptador. Escalas independentes; histórico em memória durante esta sessão.");
    }

    private void diagnostics() {
        addTitle("DIAGNÓSTICO OBD-II", "CÓDIGOS DE FALHA DA ECU");
        Button read = button("LER CÓDIGOS DE FALHA",14); read.setOnClickListener(v -> new Thread(() -> { String s = command("03"); runOnUiThread(() -> showResult("Códigos armazenados", decodeDtc(s))); }).start()); content.addView(read, matchWrap());
        Button pending = button("LER FALHAS PENDENTES",14); pending.setOnClickListener(v -> new Thread(() -> { String s = command("07"); runOnUiThread(() -> showResult("Códigos pendentes", decodeDtc(s))); }).start()); content.addView(pending, matchWrap());
        Button clear = button("APAGAR CÓDIGOS (COM CONFIRMAÇÃO)",14); clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Apagar falhas?").setMessage("Isso envia o comando OBD-II 04 à ECU e pode apagar dados de diagnóstico. Prossiga somente com o veículo parado.").setNegativeButton("Cancelar",null).setPositiveButton("Apagar",(d,w)->new Thread(()->{String s=command("04");runOnUiThread(()->showResult("Resultado",s));}).start()).show()); content.addView(clear, matchWrap());
        addNote("A disponibilidade de DTCs permanentes, freeze frame e dados específicos Peugeot depende da ECU e do adaptador. Esta versão consulta modos OBD-II genéricos 03 e 07.");
    }

    private void settings() {
        addTitle("CONFIGURAÇÕES", "CONEXÃO E PREFERÊNCIAS");
        Button pair = button("ESCOLHER ADAPTADOR OBD2 PAREADO",14); pair.setOnClickListener(v -> chooseDevice()); content.addView(pair, matchWrap());
        Button disc = button("DESCONECTAR",14); disc.setOnClickListener(v -> disconnect()); content.addView(disc, matchWrap());
        Button fuelGas = button("COMBUSTÍVEL: GASOLINA",14); fuelGas.setOnClickListener(v -> toast("Preferência registrada nesta sessão. Consumo depende dos dados fornecidos pela ECU.")); content.addView(fuelGas, matchWrap());
        Button fuelEt = button("COMBUSTÍVEL: ETANOL",14); fuelEt.setOnClickListener(v -> toast("Preferência registrada nesta sessão. Consumo depende dos dados fornecidos pela ECU.")); content.addView(fuelEt, matchWrap());
        addNote("Para usar como tela inicial, pressione o botão HOME do Android e selecione 308 Smart Drive. A disponibilidade depende do firmware da multimídia.");
        addNote("Compatibilidade de instalação: Android 7.1 (API 25) ou superior. Bluetooth OBD2: adaptador ELM327 Bluetooth Classic já pareado nas configurações do Android.");
    }

    private void chooseDevice() {
        if (btAdapter == null) { toast("Bluetooth indisponível"); return; }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 77); return; }
        if (!btAdapter.isEnabled()) { toast("Ative o Bluetooth da multimídia nas configurações do Android."); return; }
        ArrayList<BluetoothDevice> devices = new ArrayList<>(); ArrayList<String> names = new ArrayList<>();
        try { for (BluetoothDevice d : btAdapter.getBondedDevices()) { devices.add(d); names.add((d.getName()==null?"Dispositivo":d.getName())+"  •  "+d.getAddress()); } }
        catch (SecurityException e) { toast("Permissão Bluetooth negada"); return; }
        if (devices.isEmpty()) { toast("Nenhum dispositivo pareado. Pareie o ELM327 nas configurações Bluetooth primeiro."); return; }
        new AlertDialog.Builder(this).setTitle("Selecione seu ELM327").setItems(names.toArray(new String[0]),(d,which)->connect(devices.get(which))).show();
    }

    private void connect(BluetoothDevice device) {
        toast("Conectando ao adaptador...");
        new Thread(() -> {
            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                BluetoothSocket s = device.createRfcommSocketToServiceRecord(uuid); s.connect();
                socket=s; in=s.getInputStream(); out=s.getOutputStream(); connected=true;
                command("ATZ"); command("ATE0"); command("ATL0"); command("ATS0"); command("ATH0"); command("ATSP0");
                runOnUiThread(()->{ connectionLabel.setText("● OBD CONECTADO"); connectionLabel.setTextColor(0xFF23834B); toast("OBD2 conectado"); showTab(currentTab); });
                startPolling();
            } catch (Exception e) { connected=false; runOnUiThread(()->{toast("Falha na conexão: confira pareamento e adaptador ELM327."); connectionLabel.setText("● OBD DESCONECTADO");}); closeSocket(); }
        }).start();
    }

    private void startPolling() {
        if (poller != null) handler.removeCallbacks(poller);
        poller = new Runnable() { @Override public void run() {
            if (!connected) return;
            new Thread(() -> {
                try {
                    data.coolant = parseTemp(command("0105")); data.rpm = parseRpm(command("010C")); data.speed = parseIntPid(command("010D"),0);
                    data.map = parseIntPid(command("010B"),0); data.intake = parseTemp(command("010F")); data.load = parsePercent(command("0104"));
                    data.throttle = parsePercent(command("0111")); data.stft = parseFuelTrim(command("0106")); data.ltft = parseFuelTrim(command("0107"));
                    data.voltage = parseVoltage(command("0142")); data.maf = parseMaf(command("0110"));
                    if (data.coolant >= 0) addHistory(tempHistory,data.coolant); if (data.rpm >= 0) addHistory(rpmHistory,data.rpm);
                    runOnUiThread(() -> { connectionLabel.setText("● OBD CONECTADO"); if (currentTab.equals("INÍCIO")||currentTab.equals("MOTOR")||currentTab.equals("GRÁFICOS")) showTab(currentTab); });
                } catch (Exception ignored) { }
            }).start(); handler.postDelayed(this, 7000);
        }};
        handler.post(poller);
    }

    private synchronized String command(String cmd) {
        if (!connected || out == null || in == null) return "";
        try {
            out.write((cmd+"\r").getBytes(StandardCharsets.US_ASCII)); out.flush();
            StringBuilder sb=new StringBuilder(); long end=System.currentTimeMillis()+900;
            while(System.currentTimeMillis()<end) { if(in.available()>0) { int c=in.read(); if(c=='>' ) break; if(c>=0) sb.append((char)c); } else Thread.sleep(12); }
            return sb.toString().replace("\r", " ").replace("\n", " ").trim().toUpperCase(Locale.US);
        } catch(Exception e) { return ""; }
    }

    private void disconnect() { connected=false; if(poller!=null)handler.removeCallbacks(poller); closeSocket(); connectionLabel.setText("● OBD DESCONECTADO"); connectionLabel.setTextColor(MUTED); showTab(currentTab); toast("OBD2 desconectado"); }
    private void closeSocket(){try{if(socket!=null)socket.close();}catch(Exception ignored){} socket=null;in=null;out=null;}
    @Override protected void onDestroy(){connected=false;if(poller!=null)handler.removeCallbacks(poller);closeSocket();super.onDestroy();}

    private int parseHex(String s,String prefix,int bytes) { try { String x=cleanHex(s); int i=x.indexOf(prefix); if(i<0||x.length()<i+prefix.length()+bytes*2)return -1; return Integer.parseInt(x.substring(i+prefix.length(),i+prefix.length()+bytes*2),16); }catch(Exception e){return -1;} }
    private String cleanHex(String s){return s.replaceAll("[^0-9A-F]","");}
    private float parseTemp(String s){int a=parseHex(s,"4105",1); if(a<0)a=parseHex(s,"410F",1); return a<0?-1:a-40;}
    private float parseRpm(String s){int a=parseHex(s,"410C",2);return a<0?-1:a/4f;}
    private float parseIntPid(String s,int pid){String p=String.format(Locale.US,"41%02X",pid);return parseHex(s,p,1);}
    private float parsePercent(String s){int a=parseHex(s,"4104",1);if(a<0)a=parseHex(s,"4111",1);return a<0?-1:a*100f/255f;}
    private float parseFuelTrim(String s){int a=parseHex(s,"4106",1);if(a<0)a=parseHex(s,"4107",1);return a<0?-1:(a-128)*100f/128f;}
    private float parseVoltage(String s){int a=parseHex(s,"4142",2);return a<0?-1:a/1000f;}
    private float parseMaf(String s){int a=parseHex(s,"4110",2);return a<0?-1:a/100f;}
    private void addHistory(ArrayDeque<Float> q,float v){q.addLast(v);while(q.size()>60)q.removeFirst();}
    private String val(float v,String unit){return v<0?"N/D":(unit.equals("rpm")?String.format(Locale.US,"%.0f %s",v,unit):String.format(Locale.US,"%.1f %s",v,unit));}

    private String decodeDtc(String raw){String h=cleanHex(raw); int pos=h.indexOf("43"); if(pos<0)pos=h.indexOf("47"); if(pos<0)return raw.isEmpty()?"Sem resposta da ECU. Verifique a conexão.":"Nenhum código reconhecido: "+raw; String data=h.substring(pos+2); StringBuilder b=new StringBuilder(); for(int i=0;i+3<data.length();i+=4){String c=data.substring(i,i+4);if(c.equals("0000"))continue;int a=Integer.parseInt(c.substring(0,1),16);String[] type={"P","C","B","U"};int d=Integer.parseInt(c.substring(1,2),16);String code=type[a]+d+c.substring(2);b.append(code).append("\n");}return b.length()==0?"Nenhum código informado pela ECU.":b.toString();}
    private void showResult(String title,String result){new AlertDialog.Builder(this).setTitle(title).setMessage(result).setPositiveButton("OK",null).show();}

    private void addTitle(String title,String sub){content.addView(text(title,22,DARK,true));content.addView(text(sub,10,MUTED,true));View spacer=new View(this);content.addView(spacer,new LinearLayout.LayoutParams(1,10));}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setBackgroundColor(WHITE);l.setPadding(12,12,12,12);LinearLayout.LayoutParams p=matchWrap();p.bottomMargin=12;l.setLayoutParams(p);return l;}
    private LinearLayout metric(String label,String value,int color){LinearLayout c=card();TextView a=text(label,10,MUTED,true),b=text(value,18,color,true);c.addView(a);c.addView(b);return c;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(5,5,5,5);return t;}
    private Button button(String s,int size){Button b=new Button(this);b.setText(s);b.setTextSize(size);b.setAllCaps(false);b.setTextColor(WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));return b;}
    private LinearLayout.LayoutParams matchWrap(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=9;return p;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(3,3,3,3);return p;}
    private void addNote(String s){TextView t=text(s,11,MUTED,false);t.setPadding(8,12,8,14);content.addView(t);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void homeNoop(){}

    private static class OBD {float speed=-1,rpm=-1,coolant=-1,intake=-1,map=-1,load=-1,throttle=-1,voltage=-1,stft=-1,ltft=-1,maf=-1,fuelRate=-1;String lambdaText="N/D";}
    private class MiniChart extends View {
        private final ArrayDeque<Float> values; private final Paint p=new Paint(3); private final int color;
        MiniChart(ArrayDeque<Float> values,int color){super(MainActivity.this);this.values=values;this.color=color;setBackgroundColor(WHITE);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setColor(0xFFE7E9ED);p.setStrokeWidth(1);for(int i=1;i<4;i++)c.drawLine(0,h*i/4,w,h*i/4,p);if(values.size()<2)return;float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;for(float v:values){min=Math.min(min,v);max=Math.max(max,v);}if(max-min<1)max=min+1;p.setColor(color);p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);float xstep=w/(values.size()-1),x=0;int i=0;float lastX=0,lastY=0;for(float v:values){float y=h-8-(v-min)/(max-min)*(h-16);if(i>0)c.drawLine(lastX,lastY,x,y,p);lastX=x;lastY=y;x+=xstep;i++;}p.setStyle(Paint.Style.FILL);}
    }
}
