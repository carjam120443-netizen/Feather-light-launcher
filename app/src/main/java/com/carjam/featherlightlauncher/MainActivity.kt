package com.carjam.featherlightlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var root: SwipeRoot
    private lateinit var content: LinearLayout
    private lateinit var clock: TextView
    private lateinit var drawerButton: TextView
    private var showingDrawer = false
    private var apps: List<AppInfo> = emptyList()
    private val recentApps = LinkedHashMap<String, AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)); apps = loadApps(); buildUi(); showHome() }
    override fun onResume() { super.onResume(); if (::root.isInitialized) { root.background = selectedWallpaper(); if (!showingDrawer) showHome() } }

    private fun selectedWallpaper(): android.graphics.drawable.Drawable {
        val name = getSharedPreferences("launcher", MODE_PRIVATE).getString("wallpaper", "midnight") ?: "midnight"
        val res = when (name) { "blue" -> R.drawable.feather_blue; "color" -> R.drawable.feather_color; "aurora" -> R.drawable.feather_aurora; "minimal" -> R.drawable.feather_minimal; else -> R.drawable.feather_midnight }
        return getDrawable(res) ?: ColorDrawable(Color.rgb(20, 24, 32))
    }

    private fun buildUi() {
        root = SwipeRoot(this).apply { onSwipeUp={if(!showingDrawer)showDrawer()}; onSwipeDown={if(showingDrawer)showHome()}; orientation=LinearLayout.VERTICAL; background=selectedWallpaper() }
        val overlay=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,30,20,8);setBackgroundColor(Color.argb(25,0,0,0))}; root.addView(overlay,LinearLayout.LayoutParams(-1,0,1f))
        clock=TextView(this).apply{textSize=52f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setShadowLayer(8f,0f,2f,Color.BLACK)}; overlay.addView(clock,LinearLayout.LayoutParams(-1,-2))
        content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; overlay.addView(content,LinearLayout.LayoutParams(-1,0,1f))
        drawerButton=TextView(this).apply{text="⌃   Apps";textSize=16f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setShadowLayer(5f,0f,1f,Color.BLACK);setPadding(12,14,12,14);isClickable=true;setOnClickListener{if(showingDrawer)showHome()else showDrawer()}}; overlay.addView(drawerButton,LinearLayout.LayoutParams(-1,58));setContentView(root)
    }

    private fun showHome(){showingDrawer=false;drawerButton.text="⌃   Apps";content.removeAllViews();content.addView(TextView(this).apply{text="🪶";textSize=38f;gravity=Gravity.CENTER;setPadding(8,0,8,0)});content.addView(TextView(this).apply{text=SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(Date());textSize=17f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setShadowLayer(5f,0f,1f,Color.BLACK);setPadding(8,0,8,10)});val battery=getSystemService(BATTERY_SERVICE)as android.os.BatteryManager;content.addView(TextView(this).apply{text="🔋 ${battery.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}%";textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setShadowLayer(4f,0f,1f,Color.BLACK)});content.addView(TextView(this).apply{text="Recent apps";textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setShadowLayer(4f,0f,1f,Color.BLACK);setPadding(8,8,8,2)});content.addView(createDock());content.addView(TextView(this).apply{text="⚙ Launcher settings";textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setShadowLayer(4f,0f,1f,Color.BLACK);setPadding(8,8,8,8);setOnClickListener{startActivity(Intent(this@MainActivity,LauncherSettingsActivity::class.java))}});content.addView(TextView(this).apply{text="Swipe up for apps";textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setShadowLayer(4f,0f,1f,Color.BLACK)},LinearLayout.LayoutParams(-1,0,1f))}

    private fun createDock():View{val theme=getSharedPreferences("launcher",MODE_PRIVATE).getString("dock_theme","glass") ?: "glass";val dock=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;setPadding(12,8,12,8);elevation=12f;background=when(theme){"windows"->GradientDrawable().apply{setColor(Color.argb(210,245,245,245));cornerRadius=18f};"mac"->GradientDrawable().apply{setColor(Color.argb(185,245,245,245));cornerRadius=28f};"aero"->GradientDrawable().apply{setColor(Color.argb(125,225,240,255));cornerRadius=18f};else->GradientDrawable().apply{setColor(Color.argb(115,35,35,35));cornerRadius=24f}}};val items=recentApps.values.toList().takeLast(6).reversed();if(items.isEmpty())dock.addView(TextView(this).apply{text="Apps you open will appear here";textSize=13f;setTextColor(if(theme=="windows"||theme=="mac")Color.DKGRAY else Color.WHITE);gravity=Gravity.CENTER;setPadding(16,12,16,12)})else items.forEach{dock.addView(createDockAppView(it,theme))};return dock}
    private fun createDockAppView(app:AppInfo,theme:String):View{return ImageView(this).apply{setImageDrawable(app.icon);scaleType=ImageView.ScaleType.CENTER_INSIDE;contentDescription=app.label;isClickable=true;setPadding(8,6,8,6);setOnClickListener{launchApp(app)}}.also{it.layoutParams=LinearLayout.LayoutParams(64,64)}}

    private fun showDrawer(){showingDrawer=true;drawerButton.text="⌄   Home";content.removeAllViews();val search=EditText(this).apply{hint="Search apps";setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setPadding(18,8,18,8);setBackgroundColor(Color.argb(100,0,0,0))};content.addView(search,LinearLayout.LayoutParams(-1,58));val scroll=ScrollView(this);val grid=GridLayout(this).apply{columnCount=4;useDefaultMargins=true;setPadding(0,12,0,20)};scroll.addView(grid,ViewGroup.LayoutParams(-1,-2));content.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));fun render(filter:String){grid.removeAllViews();val q=filter.trim().lowercase(Locale.getDefault());apps.asSequence().filter{q.isEmpty()||searchScore(it.label,q)>0}.sortedWith(compareByDescending<AppInfo>{searchScore(it.label,q)}.thenBy{it.label.lowercase(Locale.getDefault())}).forEach{app->grid.addView(createAppView(app,76),GridLayout.LayoutParams().apply{width=0;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})}};render("");search.addTextChangedListener(SimpleTextWatcher{render(it)})}
    private fun searchScore(label:String,query:String):Int{if(query.isEmpty())return 1;val name=label.lowercase(Locale.getDefault());if(name==query)return 100;if(name.startsWith(query))return 80;if(name.split(" ","-","_").any{it.startsWith(query)})return 65;if(name.contains(query))return 50;var i=0;for(c in name)if(i<query.length&&c==query[i])i++;return if(i==query.length)25 else 0}
    private fun createAppView(app:AppInfo,size:Int):View{val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;isClickable=true;isFocusable=true;setPadding(4,6,4,6);setOnClickListener{launchApp(app)}};box.addView(ImageView(this).apply{setImageDrawable(app.icon);scaleType=ImageView.ScaleType.CENTER_INSIDE},LinearLayout.LayoutParams(size,size));box.addView(TextView(this).apply{text=app.label;textSize=12f;gravity=Gravity.CENTER;maxLines=2;setTextColor(Color.WHITE);setShadowLayer(4f,0f,1f,Color.BLACK)},LinearLayout.LayoutParams(-1,-2));return box}
    private fun loadApps():List<AppInfo>{val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);return packageManager.queryIntentActivities(intent,0).mapNotNull{r->val i=r.activityInfo?:return@mapNotNull null;AppInfo(i.loadLabel(packageManager).toString(),i.loadIcon(packageManager),i.packageName,i.name)}.distinctBy{"${it.packageName}/${it.activityName}"}.sortedBy{it.label.lowercase(Locale.getDefault())}}
    private fun launchApp(app:AppInfo){recentApps.remove("${app.packageName}/${app.activityName}");recentApps["${app.packageName}/${app.activityName}"]=app;runCatching{startActivity(Intent(Intent.ACTION_MAIN).apply{addCategory(Intent.CATEGORY_LAUNCHER);setClassName(app.packageName,app.activityName)})}}
    data class AppInfo(val label:String,val icon:android.graphics.drawable.Drawable,val packageName:String,val activityName:String)
}

private class SwipeRoot(context:android.content.Context):LinearLayout(context){var onSwipeUp:(()->Unit)?=null;var onSwipeDown:(()->Unit)?=null;private var startX=0f;private var startY=0f;private var tracking=false;private var triggered=false;private val slop=ViewConfiguration.get(context).scaledTouchSlop;override fun onInterceptTouchEvent(ev:MotionEvent):Boolean{when(ev.actionMasked){MotionEvent.ACTION_DOWN->{startX=ev.x;startY=ev.y;tracking=false;triggered=false};MotionEvent.ACTION_MOVE->{val dx=ev.x-startX;val dy=ev.y-startY;if(kotlin.math.abs(dy)>slop*1.25f&&kotlin.math.abs(dy)>kotlin.math.abs(dx)*1.08f){tracking=true;return true}}};return tracking};override fun onTouchEvent(ev:MotionEvent):Boolean{when(ev.actionMasked){MotionEvent.ACTION_DOWN->{startX=ev.x;startY=ev.y;tracking=true;triggered=false;return true};MotionEvent.ACTION_MOVE->{if(tracking&&!triggered){val dy=ev.y-startY;if(kotlin.math.abs(dy)>=28f){triggered=true;if(dy<0)onSwipeUp?.invoke()else onSwipeDown?.invoke()}};return true};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{tracking=false;return true}};return true}}
private class SimpleTextWatcher(private val callback:(String)->Unit):android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int)=callback(s?.toString().orEmpty());override fun afterTextChanged(s:android.text.Editable?)=Unit}