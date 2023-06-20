package com.example.receiptskeeper.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receiptskeeper.R;
import com.example.receiptskeeper.classes.Customer;
import com.example.receiptskeeper.fragment.DetailsFragment;
import com.example.receiptskeeper.utils.Utility;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {
    private Context context;
    private List<Customer> list;
    private List<Boolean> havingReceiptList;
    private FirebaseUser currentUser;
    private String currentUserId;

    public QueueAdapter(@NonNull Context context, @NonNull List<Customer> list, @NonNull List<Boolean> havingReceiptList) {
        this.havingReceiptList = havingReceiptList;
        this.context = context;
        this.list = list;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (currentUser != null) ? currentUser.getUid() : null;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView queueCustomerName;
        TextView queueHour;
        LinearLayout btnDetails;
        ImageButton btnDelete;
        MaterialCardView queueCardContainer;

        public ViewHolder(View view) {
            super(view);
            queueCustomerName = view.findViewById(R.id.queueCustomerName);
            queueHour = view.findViewById(R.id.queueHour);
            btnDetails = view.findViewById(R.id.btnDetails);
            btnDelete = view.findViewById(R.id.btnDelete);
            queueCardContainer = view.findViewById(R.id.queueCardContainer);
        }
    }

    @NonNull
    @Override
    public QueueAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.queue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Customer currentCustomer = list.get(position);
        Boolean isHavingReceipt = havingReceiptList.get(position);
        if (isHavingReceipt) {
            setupColorIfHasReceipt(holder.queueCardContainer);
        } else {
            setupColorIfNoReceipt(holder.queueCardContainer);
        }
        setupCustomerInfo(currentCustomer, holder.queueCustomerName, holder.queueHour);
        setupDetailsButton(currentCustomer, holder.btnDetails);
        setupDeleteButton(currentCustomer, holder.btnDelete);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void setupColorIfHasReceipt(MaterialCardView btnDetails) {
        int color = context.getResources().getColor(R.color.queuesWithRecipts,context.getTheme());
        btnDetails.setCardBackgroundColor(color);
    }

    private void setupColorIfNoReceipt(MaterialCardView queueCardContainer) {
        int color = context.getResources().getColor(R.color.queuesNoRecipts, context.getTheme());
        queueCardContainer.setCardBackgroundColor(color);
    }

    private void setupCustomerInfo(Customer customer, TextView queueCustomerName, TextView queueHour) {
        queueCustomerName.setText(customer.getName());
        queueHour.setText(customer.getHour() + "");
    }

    private void setupDetailsButton(Customer currentCustomer, LinearLayout btnDetails) {
        btnDetails.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("customerKey",  currentCustomer.getHour().replace(":", "") + currentCustomer.getName());
            bundle.putInt("keyYear", currentCustomer.getYear());
            bundle.putInt("keyMonth", currentCustomer.getMonth());
            bundle.putInt("keyDay", currentCustomer.getDay());

            // creates an instance of DetailsFragment, sets its arguments, starts a transaction to
            // replace the current fragment with DetailsFragment, adds the transaction to the back stack,
            // and commits the transaction to display the new fragment.

            //creates a new instance of the DetailsFragment class
            DetailsFragment detailsFragment = new DetailsFragment();
            //sets the provided bundle as the arguments for the detailsFragment.
            //These arguments can be accessed within the DetailsFragment to pass data or configuration.
            detailsFragment.setArguments(bundle);
            //retrieves the FragmentManager associated with the current FragmentActivity.
            FragmentManager fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
            //starts a new fragment transaction using the obtained FragmentManager.
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            // places the contents of the specified container view (identified by the R.id.fragmentContainerView resource ID)
            // with the detailsFragment. This effectively replaces the current fragment in the container with
            fragmentTransaction.replace(R.id.fragmentContainerView, detailsFragment);
            //dds the transaction to the fragment back stack. By adding it to the back stack,
            // the transaction is reversible, allowing the user to navigate back to the previous
            // fragment by pressing the back button.
            fragmentTransaction.addToBackStack(null);
            //commits the transaction, applying the changes and displaying the
            // DetailsFragment in the specified container view.
            fragmentTransaction.commit();
        });
    }

    private void setupDeleteButton(Customer customer, ImageButton btnDelete) {
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(customer));
    }

    private void showDeleteConfirmationDialog(Customer customer) {
        new AlertDialog.Builder(context)
                .setTitle("מחק תור")
                .setMessage("אתה בטוח שאתה רוצה למחוק את התור")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteQueue(customer))
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteQueue(Customer customer) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = generateDocRef(db, customer);
        Log.d("DeleteFile2", "File deleted: " + docRef.getPath());

        // Initialize batch
        WriteBatch batch = db.batch();

        // Delete image from Firebase Storage
        deleteImageFromStorage(customer);

        // Delete document from Firestore
        batch.delete(docRef);

        // Commit the batch
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                list.remove(customer);
                notifyDataSetChanged();
                Utility.showToast(context,"התור נמחק");
            } else {
                Utility.showToast(context,"שגיאה במחיקת התור ");
            }
        });
    }

    private void deleteImageFromStorage(Customer customer) {
        if (customer == null || currentUserId == null || currentUserId.isEmpty()) {
            Log.e("DeleteFile", "Customer or User ID is null or empty");
            Utility.showToast(context,"שגיאה במחיקת התור ");
            return;
        }

        // Initialize the Firebase Storage instance
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();

        // Formulate the path to the image based on the customer details
        String year = String.valueOf(customer.getYear());
        String month = String.valueOf(customer.getMonth());
        String day = String.valueOf(customer.getDay());
        String customerKey = customer.getHour().replace(":", "") +customer.getName();

        // Create a storage reference to the image
        StorageReference storageRef = firebaseStorage.getReference().child(currentUserId).child(year).child(month)
                .child(day).child("DayQueues").child(customerKey);

        storageRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        item.delete()
                                .addOnSuccessListener(unused -> {
                                })
                                .addOnFailureListener(e ->
                                        Utility.showToast(context,"שגיאה במחחיקת התמונה שבתור"));
                    }
                })
                .addOnFailureListener(e -> Utility.showToast(context,"שגיאה ברישום קבצים"));
    }

    private DocumentReference generateDocRef(FirebaseFirestore db, Customer customer) {
        DocumentReference pathRefWithDate = db.collection(currentUserId).document( customer.getYear() +"").collection(customer.getMonth()+"").document(customer.getDay()+"");
        String customerId =  customer.getHour().replace(":", "") +customer.getName();
        return pathRefWithDate
                .collection("DayQueues")
                .document(customerId);
    }
}
