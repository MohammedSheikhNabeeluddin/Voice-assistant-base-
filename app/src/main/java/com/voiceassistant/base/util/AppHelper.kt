<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/titleText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Voice Assistant"
        android:textSize="24sp"
        android:textStyle="bold"
        android:gravity="center"
        android:layout_marginBottom="16dp"/>

    <TextView
        android:id="@+id/statusText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Status: Checking..."
        android:textSize="14sp"
        android:padding="12dp"
        android:background="#E0E0E0"
        android:layout_marginBottom="16dp"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginBottom="8dp">

        <Button
            android:id="@+id/startButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Start"
            android:layout_marginEnd="4dp"/>

        <Button
            android:id="@+id/stopButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Stop"
            android:layout_marginStart="4dp"/>
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginBottom="16dp">

        <Button
            android:id="@+id/permissionButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Permissions"
            android:textSize="12sp"
            android:layout_marginEnd="4dp"/>

        <Button
            android:id="@+id/accessibilityButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Accessibility"
            android:textSize="12sp"
            android:layout_marginStart="4dp"/>
    </LinearLayout>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Conversation:"
        android:textSize="16sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp"/>

    <ScrollView
        android:id="@+id/transcriptScroll"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:background="#F5F5F5"
        android:padding="8dp">

        <TextView
            android:id="@+id/transcriptText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="14sp"/>
    </ScrollView>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Say 'Hey Assistant' to activate"
        android:textSize="12sp"
        android:gravity="center"
        android:layout_marginTop="8dp"
        android:textColor="#666666"/>

</LinearLayout>
