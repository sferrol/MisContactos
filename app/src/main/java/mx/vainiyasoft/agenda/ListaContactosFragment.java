package mx.vainiyasoft.agenda;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import mx.vainiyasoft.agenda.data.ContactArrayAdapter;
import mx.vainiyasoft.agenda.data.ContactReceiver;
import mx.vainiyasoft.agenda.entity.Contacto;
import mx.vainiyasoft.agenda.entity.ContactoContract;
import mx.vainiyasoft.agenda.net.NetworkBridge;
import mx.vainiyasoft.agenda.util.MenuBarActionReceiver;

import static mx.vainiyasoft.agenda.util.MenuBarActionReceiver.FILTER_NAME;
import static mx.vainiyasoft.agenda.util.MenuBarActionReceiver.MenuBarActionListener;

/**
 * Created by alejandro on 5/2/14.
 */
public class ListaContactosFragment extends ListFragment implements MenuBarActionListener {

    private static final String LOG_TAG = ListaContactosFragment.class.getSimpleName();

    private ListView listView;

    private MenuBarActionReceiver receiver;
    private ContactArrayAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            Context context = getActivity();
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(ContactoContract.CONTENT_URI, null, null, null, null);
            List<Contacto> contactos = Contacto.crearListaDeCursor(cursor);
            // El adapter será el encargado de ir creando los fragmentos conforme se necesiten y "reciclarlos"
            listAdapter = new ContactArrayAdapter(context, R.layout.listview_item, contactos);
            setListAdapter(listAdapter);
            cursor.close();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true); // Habilita el ActionBAr de este fragment para tener botones
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); // Cambiar a CHOICE_MODE_MULTIPLE_MODAL al utilizar Contextual ActionBAr (CAB)
        listView.setDividerHeight(5); // Usabamos 5dp en el xml
        listView.setBackgroundColor(0xFFD1D1D1);
        listView.setDivider(null); // Remover las separaciones entre elementos de la lista
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new MenuBarActionReceiver(this);
        // Sólo recibirá notificaciones mientras se encuentre mostrando en pantalla
        getActivity().registerReceiver(receiver, new IntentFilter(FILTER_NAME));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
    }

    @Override
    public void contactoAgregado(Contacto contacto) {
        listAdapter.add(contacto);
    }

    @Override
    public void eliminarContactos() {
        String mensaje = "¿Está seguro de eliminar los contactos seleccionados?";
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.ic_action_warning);
        builder.setTitle(i18n(R.string.title_alertdialog_confirm));
        builder.setMessage(i18n(R.string.mesg_confirm_delete));
        builder.setPositiveButton(i18n(R.string.mesg_positive_dialog_option), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ArrayList<Contacto> seleccion = new ArrayList<Contacto>();
                for(int position : listAdapter.getCurrentCheckedPositions()) {
                    if(listAdapter.isPositionChecked(position)) seleccion.add(listAdapter.getItem(position));
                }
                for (Contacto con : seleccion) listAdapter.remove(con);
                Intent intent = new Intent(ContactReceiver.FILTER_NAME);
                intent.putExtra("operacion", ContactReceiver.CONTACTO_ELIMINADO);
                intent.putParcelableArrayListExtra("datos", seleccion);
                getActivity().sendBroadcast(intent);
                listView.clearChoices();
            }
        });
        builder.setNegativeButton(i18n(R.string.mesg_negative_dialog_option), null);
        builder.show();
    }

    @Override
    public void sincronizarDatos() {
        NetworkBridge bridge = new NetworkBridge(getActivity());
        bridge.sincronizarDatos();
    }

    // Comentamos este método, pues lo utilizaremos más adelante.
//    @OnItemLongClick(R.id.fragment_listview)
//    public boolean onItemLongClick(int position) {
//        ShareOptionsBridge bridge = new ShareOptionsBridge(listAdapter, getActivity());
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle(R.string.share_title);
//        builder.setItems(R.array.share_options, bridge.getDialogOnClickListener(position));
//        AlertDialog dialog = builder.create();
//        dialog.show();
//        return true;
//    }

    private String i18n(int resourceId, Object... formatArgs) {
        return getResources().getString(resourceId, formatArgs);
    }

}
