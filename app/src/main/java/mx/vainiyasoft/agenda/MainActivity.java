package mx.vainiyasoft.agenda;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;
import mx.vainiyasoft.agenda.data.ContactOperations;
import mx.vainiyasoft.agenda.nav.DrawerAdapter;
import mx.vainiyasoft.agenda.net.HttpServiceBroker;
import mx.vainiyasoft.agenda.util.SweeperTask;

import static android.gesture.GestureOverlayView.OnGesturePerformedListener;

// Ya no se usa el OrmLiteBaseActivity al usar ContentProvider
public class MainActivity extends Activity implements OnGesturePerformedListener {

    @InjectView(R.id.rootPane)
    protected DrawerLayout drawerLayout;
    @InjectView(R.id.nav_drawer)
    protected ListView drawerList;

    private ActionBarDrawerToggle drawerToggle;
    private CharSequence mDrawerTitle, mTitle;
    private String[] titulos;

    private CrearContactoFragment fragmentoCrear;
    private ListaContactosFragment fragmentoLista;
    private GestureLibrary gestureLib;
    private final int CONFIG_REQUEST_CODE = 0;

    private ContactOperations operations;
    private HttpServiceBroker broker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View overlayView = inicializarVista();
        setContentView(overlayView);
        titulos = getResources().getStringArray(R.array.nav_drawer_titles);
        ButterKnife.inject(this);
        DrawerAdapter adapter = new DrawerAdapter(this, R.layout.drawer_item, titulos);
        View listHeader = View.inflate(this, R.layout.drawer_list_header, null);
        drawerList.addHeaderView(listHeader, null, false);
        drawerList.setAdapter(adapter);
        inicializarNavigationDrawer();
        inicializaComponentes();
        requestBackup();
    }

    private void requestBackup() {
        BackupManager manager = new BackupManager(this);
        manager.dataChanged();
    }

    private void inicializarNavigationDrawer() {
        mTitle = mDrawerTitle = getTitle();
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_navigation_drawer,
                    R.string.drawer_open, R.string.drawer_close) {
                @Override
                public void onDrawerOpened(View drawerView) {
                    // Puliendo la app, ocultamos el teclado virtual cuando no se utiliza
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(drawerView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    super.onDrawerOpened(drawerView);
                    actionBar.setTitle(mDrawerTitle);
                    invalidateOptionsMenu();
                }

                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    actionBar.setTitle(mTitle);
                    invalidateOptionsMenu();
                }
            };
            // Asignamos el objeto drawerToggle como el DrawerListener
            drawerLayout.setDrawerListener(drawerToggle);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sincronizamos el estado del drawerToggle después de que se ejecute el método onRestoreInstanceState
        if (drawerToggle != null) drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) drawerToggle.onConfigurationChanged(newConfig);
    }

    private View inicializarVista() {
        GestureOverlayView overlay = new GestureOverlayView(this);
        View inflate = getLayoutInflater().inflate(R.layout.activity_main, null);
        overlay.addView(inflate);
        overlay.addOnGesturePerformedListener(this);
        overlay.setGestureVisible(false);
        gestureLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
        gestureLib.load();
        return overlay;
    }

    @Override
    public void onResume() {
        super.onResume();
        operations = new ContactOperations();
        operations.addContactOperationsListener(fragmentoLista);
        broker = new HttpServiceBroker();
        registerReceiver(operations, new IntentFilter(ContactOperations.FILTER_NAME));
        registerReceiver(broker, new IntentFilter(HttpServiceBroker.FILTER_NAME));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(operations);
        unregisterReceiver(broker);
    }

    @Override
    protected void onStop() {
        Handler handler = new Handler();
        SweeperTask task = new SweeperTask(this);
        handler.post(task);
        super.onStop();
    }

    private void inicializaComponentes() {
        View view = findViewById(R.id.rootPane);
        String viewTag = String.valueOf(view.getTag());
        // Verdadero para los TAGs "phone" y "phone_landscape"
        if (viewTag.startsWith("phone")) cargarFragmento(getFragmentoLista());
    }

    //<editor-fold desc="METODOS GET DE INICIALIZACION BAJO DEMANDA (LAZY INITIALIZATION)">
    public CrearContactoFragment getFragmentoCrear() {
        if (fragmentoCrear == null) fragmentoCrear = new CrearContactoFragment();
        return fragmentoCrear;
    }

    public ListaContactosFragment getFragmentoLista() {
        if (fragmentoLista == null) fragmentoLista = new ListaContactosFragment();
        return fragmentoLista;
    }
    //</editor-fold>

    private void cargarFragmento(Fragment fragmento) {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        // Asignamos el valor de transition para disparar la animación correcta
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.replace(R.id.contenedor, fragmento);
        // Agregamos esta transacción al "back stack". Esto significa que la transacción
        // será recordada después de que haya sido aceptada (commit), y podrá ser
        // ejecutada en reversa después cuando se obtenga del stack.
        ft.addToBackStack("pantallas");
        ft.commit();
    }

    @OnItemClick(R.id.nav_drawer)
    public void selectItem(int position) {
        // El caso 0, (posición 0) es ocupada por el header de la lista
        switch (position) {
            case 1:
                cargarFragmento(getFragmentoCrear());
                setTitle(titulos[position - 1]);
                break;
            case 2:
                cargarFragmento(getFragmentoLista());
                setTitle(titulos[position - 1]);
                break;
            case 3:
                notificarEliminarContactos();
                break;
            case 4:
                notificarSincronizacion();
                break;
        }
        // Resaltar el elemento seleciconado, actualizar el títulos y cerrar el drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(title);
    }

    @Override
    public void onGesturePerformed(GestureOverlayView gestureOverlayView, Gesture gesture) {
        ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
        for (Prediction pred : predictions) {
            if (pred.score > 1.0) {
                if (pred.name.equals("eliminar")) notificarEliminarContactos();
                else if (pred.name.equals("sincronizar")) notificarSincronizacion();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Importante atrapar primero las notificaciones del drawer
        if (drawerToggle.onOptionsItemSelected(item)) return true;
        switch (item.getItemId()) {
            case R.id.item_action_settings:
                Intent intent = new Intent(this, ConfiguracionActivity.class);
                startActivityForResult(intent, CONFIG_REQUEST_CODE);
                return true;
            case R.id.item_action_create:
                cargarFragmento(getFragmentoCrear());
                setTitle(titulos[1]);
                return true;
            case R.id.item_action_synchronize:
                notificarSincronizacion();
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIG_REQUEST_CODE) {
            SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(this);
            String username = shp.getString("username", null);
            String mesg = i18n(R.string.mesg_preferences_saved, username);
            Toast.makeText(this, mesg, Toast.LENGTH_SHORT).show();
        }
    }

    private void notificarSincronizacion() {
        Intent intent = new Intent(ContactOperations.FILTER_NAME);
        intent.putExtra("operacion", ContactOperations.ACCION_SINCRONIZAR_CONTACTOS);
        sendBroadcast(intent);
    }

    private void notificarEliminarContactos() {
        Intent intent = new Intent(ContactOperations.FILTER_NAME);
        intent.putExtra("operacion", ContactOperations.ACCION_ELIMINAR_CONTACTOS);
        sendBroadcast(intent);
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Desactivar la autodetección y obligar al uso de atributos y no de getter/setter
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper;
    }

    private String i18n(int resourceId, Object... formatArgs) {
        return getResources().getString(resourceId, formatArgs);
    }

}
