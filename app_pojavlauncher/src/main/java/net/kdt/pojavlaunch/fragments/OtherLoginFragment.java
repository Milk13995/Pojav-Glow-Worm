package net.kdt.pojavlaunch.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.kdt.mcgui.MineButton;
import com.kdt.mcgui.MineEditText;
import com.externallogin.login.AuthResult;
import com.externallogin.login.OtherLoginApi;
import com.externallogin.login.Servers;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OtherLoginFragment extends Fragment {
    public static final String TAG = "OtherLoginFragment";
    private ProgressDialog progressDialog;
    private Spinner serverSpinner;
    private MineEditText userEditText;
    private MineEditText passEditText;
    private MineButton loginButton;
    private TextView register;
    private ImageButton addServer;
    private File serversFile;
    private Servers servers;
    private List<String> serverList;
    public String currentBaseUrl;
    private String currentRegisterUrl;
    private ArrayAdapter<String> serverSpinnerAdapter;


    public OtherLoginFragment() {
        super(R.layout.fragment_other_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        serversFile = new File(Tools.DIR_GAME_HOME, "servers.json");
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        serverSpinner = view.findViewById(R.id.server_spinner);
        userEditText = view.findViewById(R.id.login_edit_email);
        passEditText = view.findViewById(R.id.login_edit_password);
        loginButton = view.findViewById(R.id.login_button);
        register = view.findViewById(R.id.register);
        addServer = view.findViewById(R.id.add_server);

        refreshServer();
        serverSpinner.setAdapter(serverSpinnerAdapter);
        serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (!Objects.isNull(servers)) {
                    for (Servers.Server server : servers.getServer()) {
                        if (server.getServerName().equals(serverList.get(i))) {
                            currentBaseUrl = server.getBaseUrl();
                            currentRegisterUrl = server.getRegister();
                            Log.e("test", "currentRegisterUrl:" + currentRegisterUrl);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        addServer.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.other_login_aut)
                    .setItems(new String[]{getString(R.string.other_login_external), getString(R.string.other_login_pass)}, (d, i) -> {
                        EditText editText = new EditText(requireContext());
                        editText.setMaxLines(1);
                        editText.setInputType(InputType.TYPE_CLASS_TEXT);
                        AlertDialog dialog1 = new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.other_login_tip)
                                .setView(editText)
                                .setPositiveButton(R.string.other_login_confirm, (dialogInterface, i1) -> {
                                    progressDialog.show();
                                    PojavApplication.sExecutorService.execute(() -> {
                                        String data = OtherLoginApi.getINSTANCE().getServeInfo(i==0?editText.getText().toString():"https://auth.mc-user.com:233/" + editText.getText().toString());
                                        requireActivity().runOnUiThread(() -> {
                                            progressDialog.dismiss();
                                            if (!Objects.isNull(data)) {
                                                try {
                                                    Servers.Server server = new Servers.Server();
                                                    JSONObject jsonObject = new JSONObject(data);
                                                    JSONObject meta = jsonObject.optJSONObject("meta");
                                                    server.setServerName(meta.optString("serverName"));
                                                    server.setBaseUrl(editText.getText().toString());
                                                    if (i == 0) {
                                                        JSONObject links = meta.optJSONObject("links");
                                                        server.setRegister(links.optString("register"));
                                                    } else {
                                                        server.setBaseUrl("https://auth.mc-user.com:233/" + editText.getText().toString());
                                                        server.setRegister("https://login.mc-user.com:233/" + editText.getText().toString() + "/loginreg");
                                                    }
                                                    if (Objects.isNull(servers)) {
                                                        servers = new Servers();
                                                        servers.setServer(new ArrayList<>());
                                                    }
                                                    servers.getServer().add(server);
                                                    Tools.write(serversFile.getAbsolutePath(), Tools.GLOBAL_GSON.toJson(servers, Servers.class));
                                                    refreshServer();
                                                    currentBaseUrl = server.getBaseUrl();
                                                    currentRegisterUrl = server.getRegister();
                                                } catch (Exception e) {
                                                    Log.e("test", e.toString());
                                                }
                                            }
                                        });
                                    });

                                })
                                .setNegativeButton(R.string.other_login_cancel, null)
                                .create();
                        if (i == 0) {
                            editText.setHint(R.string.other_login_address);
                        } else {
                            editText.setHint(R.string.other_login_setid);
                        }
                        dialog1.show();
                    })
                    .setNegativeButton(R.string.other_login_cancel, null)
                    .create();
            dialog.show();
        });
        register.setOnClickListener(v -> {
            if (!Objects.isNull(currentRegisterUrl)) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri url = Uri.parse(currentRegisterUrl);
                intent.setData(url);
                startActivity(intent);
            }
        });
        loginButton.setOnClickListener(v->{
            progressDialog.show();
            PojavApplication.sExecutorService.execute(()->{
                String user=userEditText.getText().toString();
                String pass=passEditText.getText().toString();
                if (!user.equals("")&&!pass.equals("")){
                    try {
                        OtherLoginApi.getINSTANCE().setBaseUrl(currentBaseUrl);
                        OtherLoginApi.getINSTANCE().login(user, pass, new OtherLoginApi.Listener() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                requireActivity().runOnUiThread(()->{
                                    progressDialog.dismiss();
                                    MinecraftAccount account=new MinecraftAccount();
                                    account.accessToken=authResult.getAccessToken();
                                    account.baseUrl= currentBaseUrl;
                                    account.account=userEditText.getText().toString();
                                    account.password=passEditText.getText().toString();
                                    account.expiresAt=System.currentTimeMillis()+30*60*1000;
                                    if (!Objects.isNull(authResult.getSelectedProfile())){
                                        account.username=authResult.getSelectedProfile().getName();
                                        account.profileId=authResult.getSelectedProfile().getId();
                                        ExtraCore.setValue(ExtraConstants.OTHER_LOGIN_TODO, account);
                                        Tools.swapFragment(requireActivity(), MainMenuFragment.class, MainMenuFragment.TAG, null);
                                    } else {
                                        List<String> list=new ArrayList<>();
                                        for(AuthResult.AvailableProfiles profiles:authResult.getAvailableProfiles()){
                                            list.add(profiles.getName());
                                        }
                                        String[] items=list.toArray(new String[0]);
                                        AlertDialog dialog=new AlertDialog.Builder(requireContext())
                                                .setTitle(R.string.other_login_role)
                                                .setItems(items,(d,i)->{
                                                    for(AuthResult.AvailableProfiles profiles:authResult.getAvailableProfiles()){
                                                        if(profiles.getName().equals(items[i])){
                                                            account.profileId=profiles.getId();
                                                            account.username=profiles.getName();
                                                        }
                                                    }
                                                    ExtraCore.setValue(ExtraConstants.OTHER_LOGIN_TODO, account);
                                                    Tools.swapFragment(requireActivity(), MainMenuFragment.class, MainMenuFragment.TAG, null);
                                                })
                                                .setNegativeButton(R.string.other_login_cancel,null)
                                                .create();
                                        dialog.show();
                                    }
                                });
                            }

                            @Override
                            public void onFailed(String error) {
                                requireActivity().runOnUiThread(()->{
                                    progressDialog.dismiss();
                                    AlertDialog dialog=new AlertDialog.Builder(requireContext())
                                            .setTitle(R.string.other_login_warning)
                                            .setTitle("An error occurred while logging in：\n"+error)
                                            .setPositiveButton(R.string.other_login_confirm,null)
                                            .create();
                                    dialog.show();
                                });
                            }
                        });
                    } catch (IOException e) {
                        requireActivity().runOnUiThread(()->progressDialog.dismiss());
                        Log.e("login",e.toString());
                    }
                }
            });
        });
    }

    public void refreshServer() {
        if (Objects.isNull(serverList)) {
            serverList = new ArrayList<>();
        } else {
            serverList.clear();
        }
        if (serversFile.exists()) {
            try {
                servers = new Gson().fromJson(Tools.read(serversFile.getAbsolutePath()), Servers.class);
                currentBaseUrl=servers.getServer().get(0).getBaseUrl();
                for (Servers.Server server : servers.getServer()) {
                    serverList.add(server.getServerName());
                }
            } catch (IOException e) {

            }
        }
        if (Objects.isNull(servers)) {
            serverList.add(getString(R.string.other_login_server_error));
        }
        if (Objects.isNull(serverSpinnerAdapter)) {
            serverSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, serverList);
        } else {
            serverSpinnerAdapter.notifyDataSetChanged();
        }

    }
}
