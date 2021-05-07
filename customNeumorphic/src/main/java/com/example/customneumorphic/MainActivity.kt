package com.example.customneumorphic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

/* <LinearLayout
       android:layout_width="match_parent"
       android:layout_height="0dp"
       android:layout_weight="1"
       android:background="#001f79"

       android:orientation="vertical"
       >
      <com.example.customneumorphic.NeumorphImageButton
          style="@style/Widget.Neumorph.ImageButton"
          android:layout_width="100dp"
          android:layout_height="100dp"
          android:paddingTop="16dp"
          app:neumorph_shadowElevation="4dp"
          app:neumorph_shadowColorLight="@color/upperShadow"
          app:neumorph_shadowColorDark="@color/lowerShadow"

          android:layout_gravity="center"
          app:neumorph_lightSource="rightBottom"
          />

      <com.example.customneumorphic.NeumorphCardView
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          app:neumorph_shadowElevation="4dp"
          app:neumorph_shadowColorLight="@color/upperShadow"
          app:neumorph_shadowColorDark="@color/lowerShadow"

          app:neumorph_shapeType="pressed"

          >

         <EditText
             android:layout_width="match_parent"
             android:layout_height="60dp"
             android:gravity="center"
             android:hint="Enter Amount"
             android:layout_gravity="center"
             android:background="@null"


             />


      </com.example.customneumorphic.NeumorphCardView>

      <com.example.customneumorphic.NeumorphCardView
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          app:neumorph_shadowElevation="4dp"
          app:neumorph_shadowColorLight="@color/upperShadow"
          app:neumorph_shadowColorDark="@color/lowerShadow"

          app:neumorph_shapeType="basin"

          >

         <EditText
             android:layout_width="match_parent"
             android:layout_height="60dp"
             android:gravity="center"
             android:hint="Enter Age"
             android:layout_gravity="center"
             android:background="@null"


             />


      </com.example.customneumorphic.NeumorphCardView>

      <com.example.customneumorphic.NeumorphCardView
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          app:neumorph_shadowElevation="4dp"
          app:neumorph_shadowColorLight="@color/upperShadow"
          app:neumorph_shadowColorDark="@color/lowerShadow"
          app:neumorph_shapeType="basin"

          >

         <EditText
             android:layout_width="match_parent"
             android:layout_height="60dp"
             android:gravity="center"
             android:hint="Enter Mobile"
             android:layout_gravity="center"
             android:background="@null"


             />


      </com.example.customneumorphic.NeumorphCardView>


   </LinearLayout>*/