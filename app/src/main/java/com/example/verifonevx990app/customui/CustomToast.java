package com.example.verifonevx990app.customui;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.verifonevx990app.BuildConfig;
import com.example.verifonevx990app.R;


public class CustomToast {
    private static final String TAG = "Bonushub";
    private final Context context;
    private int backColor;
    private int textColor;

    public CustomToast(Context context) {
        this.backColor = context.getResources().getColor(R.color.colorPrimaryTrans);
        this.textColor = context.getResources().getColor(R.color.white);
        this.context = context;
    }

    public static void printAppLog(String message) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, " message:  " + message);
        }

    }


    public void setBackground(int backColor) {
        this.backColor = backColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void showErrorToast(String text) {

        if (context != null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            View toastRoot = inflater.inflate(R.layout.custom_toast, null);
            LinearLayout linearLayout = toastRoot.findViewById(R.id.whole_layout);
            linearLayout.setBackgroundColor(backColor);
            TextView toastText = toastRoot.findViewById(R.id.toast_Tv);
            toastText.setText(text);
            toastText.setTextColor(textColor);
            TextView toastSuccessTv = toastRoot.findViewById(R.id.toast_successTv);
            toastSuccessTv.setText("Error");
            toastSuccessTv.setTextColor(textColor);
            Toast toast = new Toast(context);
            toast.setView(toastRoot);
            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM | Gravity.FILL_HORIZONTAL,
                    0, 0);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    public void showSuccessToast(String text) {
        if (context != null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            View toastRoot = inflater.inflate(R.layout.custom_toast, null);
            LinearLayout linearLayout = toastRoot.findViewById(R.id.whole_layout);
            linearLayout.setBackgroundColor(backColor);
            TextView toastText = toastRoot.findViewById(R.id.toast_Tv);
            toastText.setText(text);
            toastText.setTextColor(textColor);

            TextView toastSuccessTv = toastRoot.findViewById(R.id.toast_successTv);
            toastSuccessTv.setText("Success");
            toastSuccessTv.setTextColor(textColor);
            Toast toast = new Toast(context);
            toast.setView(toastRoot);
            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM | Gravity.FILL_HORIZONTAL,
                    0, 0);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    public void transactionDeclined(String message, final MessageHandler messageHandler) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        // set title
        alertDialogBuilder.setTitle("Error");
        // set dialog message
        alertDialogBuilder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        messageHandler.onTransactionDeclined();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    public interface BacthSattlementListener {
        void onYesClick();

        void onNoClick();
    }

    public interface MessageHandler {
        void onAlertOk();

        void onSwipeFallBack();

        void onEMVFallBack();

        void onTransactionDeclined();
    }

}

