package ru.brandymint.cadcam.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

/**
* Created by alexey on 12.05.14.
*/
public class EnterCommentDialogFragment extends DialogFragment {

    private boolean isClosed = false;

    public static interface EnterCommentDialogListener {
        public void onDialogClosed(String comment);
    }

    private EnterCommentDialogListener mDialogListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mDialogListener = (EnterCommentDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement EnterCommentDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setTitle(R.string.enter_comment_dialog_title)
                .setView(inflater.inflate(R.layout.dialog_enter_comment, null))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDialogClosed();
                    }
                });

        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        final View v = getDialog().findViewById(R.id.comment);
        v.post(new Runnable() {
            @Override
            public void run() {
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onDialogClosed();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onDialogClosed();
    }

    void onDialogClosed() {
        if (isClosed) return;
        String comment = "";

        Dialog d = getDialog();
        if (d != null) {
            TextView commentView = (TextView) getDialog().findViewById(R.id.comment);
            if (commentView != null) comment = commentView.getText().toString();
        }
        mDialogListener.onDialogClosed(comment);
        isClosed = true;
    }

}
