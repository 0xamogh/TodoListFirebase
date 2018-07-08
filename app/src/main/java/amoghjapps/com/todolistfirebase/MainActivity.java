package amoghjapps.com.todolistfirebase;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import amoghjapps.com.todolistfirebase.Adapter.ListItemAdapter;
import amoghjapps.com.todolistfirebase.Model.ToDo;
import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {
    List<ToDo> toDoList=new ArrayList<>();
    FirebaseFirestore database;
    RecyclerView list;
    RecyclerView.LayoutManager layoutManager;
    FloatingActionButton fab;
    public MaterialEditText title,description;//public so it can be accessed from ListAdapter
    public boolean isUpdate= false;//flag to check if it is an update or is adding new
    public String idUpdate="";//Id of item needed to be updated
    SpotsDialog dialog;
    ListItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing firestore
        database=FirebaseFirestore.getInstance();
        //
        dialog=new SpotsDialog(this);
        title=(MaterialEditText)findViewById(R.id.title);
        description=(MaterialEditText)findViewById(R.id.desp);
        fab=findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isUpdate==false){
                    setData(title.getText().toString(),description.getText().toString());
                }else{
                    updateData(title.getText().toString(),description.getText().toString());
                    isUpdate=false;//reset flag

                }

            }
        });
        list=findViewById(R.id.listTodo);
        list.setHasFixedSize(false);
        layoutManager=new LinearLayoutManager(this);
        list.setLayoutManager(layoutManager);
        //LOADING DATA FROM FIRESTORE
        loadData();

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().equals("DELETE"))
            deleteItem(item.getOrder());
        return super.onContextItemSelected(item);
    }

    private void deleteItem(int order) {
        database.collection("ToDoList")
                .document(toDoList.get(order).getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        loadData();
                    }
                });
    }

    private void loadData(){
        dialog.show();
        if(toDoList.size()>0){
            toDoList.clear();
        }
        database.collection("ToDoList")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for(DocumentSnapshot doc:task.getResult()){
                            ToDo toDo=new ToDo(doc.getString("id"),
                                               doc.getString("title"),
                                               doc.getString("description")
                                        );
                            toDoList.add(toDo);

                        }
                        adapter=new ListItemAdapter(MainActivity.this,toDoList);
                        list.setAdapter(adapter);
                    dialog.dismiss();}
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();;
                    }
                });
    }
    private void setData(String title,String description){
        //random id generation
        String id= UUID.randomUUID().toString();
        Map<String,Object> todo=new HashMap<>();
        todo.put("id",id);
        todo.put("title",title);
        todo.put("description",description);

        database.collection("ToDoList").document(id)
        .set(todo).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                loadData();
            }
        });

    }
    private void updateData(String title,String description){
        database.collection("ToDoList").document(idUpdate)
                .update("title",title,"description",description)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this,"List Updated",Toast.LENGTH_SHORT).show();

                    }
                });
        //refreshing data real time
        database.collection("ToDoList").document(idUpdate)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                        loadData();
                    }
                });
    }
}
