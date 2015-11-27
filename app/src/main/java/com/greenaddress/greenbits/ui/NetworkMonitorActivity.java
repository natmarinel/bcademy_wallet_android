package com.greenaddress.greenbits.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.GetDataMessage;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerEventListener;
import org.bitcoinj.core.PeerGroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public final class NetworkMonitorActivity extends FragmentActivity implements Observer
{

    @NonNull
    private ArrayList<PrettyPeer> peerList = new ArrayList<>();
    private ArrayAdapter<PrettyPeer> peerListAdapter;
    private String bloominfo = "";
    @Nullable
    private PeerEventListener peerListener;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_network);

        ListView view = (ListView) findViewById(R.id.peerlistview);

        view.setEmptyView(findViewById(R.id.empty_list_view));
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(uiUpdated);
        PeerGroup peerGroup = getGAService().getPeerGroup();
        if (peerGroup != null && peerListener != null) {
            peerGroup.removeEventListener(peerListener);
        }

        peerList.clear();

        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(uiUpdated, new IntentFilter("PEERGROUP_UPDATED"));

        peerList = new ArrayList<>();
        PeerGroup peerGroup = getGAService().getPeerGroup();
        if (peerGroup != null && peerGroup.isRunning()) {

            for (Peer peer : peerGroup.getConnectedPeers()) {
                peerList.add(new PrettyPeer(peer));
            }

            if (peerList.size() > 0) {
                bloominfo = peerList.get(0).peer.getBloomFilter().toString();
                TextView tview = (TextView) findViewById(R.id.bloominfo);
                tview.setText(bloominfo);
            }

            peerListAdapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, peerList);
            ListView view = (ListView) findViewById(R.id.peerlistview);
            view.setAdapter(peerListAdapter);

            peerListener = new PeerEventListener() {
                @Override
                public void onPeersDiscovered(Set<PeerAddress> peerAddresses) {

                }

                @Override
                public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {

                }

                @Override
                public void onChainDownloadStarted(Peer peer, int blocksLeft) {

                }

                @Override
                public synchronized void onPeerConnected(@NonNull final Peer peer, int peerCount) {
                    final PrettyPeer new_ppeer = new PrettyPeer(peer);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peerList.add(new_ppeer);
                            peerListAdapter.notifyDataSetChanged();

                            bloominfo = peer.getBloomFilter().toString();
                            TextView tview = (TextView) findViewById(R.id.bloominfo);
                            tview.setText(bloominfo);
                        }
                    });
                }

                @Override
                public synchronized void onPeerDisconnected(final Peer peer, int peerCount) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            PrettyPeer new_ppeer = new PrettyPeer(peer);
                            for (Iterator<PrettyPeer> it = peerList.iterator(); it.hasNext(); ) {
                                PrettyPeer ppeer = it.next();
                                if (new_ppeer.peer == ppeer.peer) {
                                    it.remove();
                                }
                            }
                            peerListAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Nullable
                @Override
                public Message onPreMessageReceived(Peer peer, Message m) {
                    return null;
                }

                @Override
                public void onTransaction(Peer peer, org.bitcoinj.core.Transaction t) {

                }

                @Nullable
                @Override
                public List<Message> getData(Peer peer, GetDataMessage m) {
                    return null;
                }
            };

            peerGroup.addEventListener(peerListener);
        }

        testKickedOut();
        if (getGAService() == null) {
            finish();
            return;
        }

        getGAApp().getConnectionObservable().addObserver(this);
    }

    private void testKickedOut() {
        if (getGAApp().getConnectionObservable().getIsForcedLoggedOut() || getGAApp().getConnectionObservable().getIsForcedTimeout()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            final Intent firstScreenActivity = new Intent(NetworkMonitorActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivity);
            finish();
        }
    }

    @NonNull
    private GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    private GaService getGAService() {
        return getGAApp().gaService;
    }

    @Override
    public void update(@NonNull final Observable observable, @NonNull final Object data) {

    }

    @Nullable
    private final BroadcastReceiver uiUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, @NonNull final Intent intent) {

            final String peerGroupIntent = intent.getExtras().getString("peergroup");
            if (peerGroupIntent != null && peerGroupIntent.equals("stopSPVSync")) {
                peerList.clear();
                peerListAdapter.notifyDataSetChanged();
            }
        }
    };

    private class PrettyPeer{
        final Peer peer;
        public PrettyPeer(final Peer peer) {
            this.peer = peer;
        }

        @NonNull
        public String toString(){
            String ipAddr = peer.toString();
            if(ipAddr.length() >= 11 && ipAddr.substring(0,11).equals("[127.0.0.1]")){
                ipAddr = getSharedPreferences("TRUSTED", MODE_PRIVATE).getString("address", "Trusted Onion");
                final Node n = new Node(ipAddr);
                ipAddr = n.toString();
            }
            return "IP Addr: "+ipAddr+"\n"+"Version: "+peer.getPeerVersionMessage().subVer+"\nBlockheight: "+
                    peer.getBestHeight();
        }
    }

    class Node {
        @NonNull
        final String addr;
        final int port;

        Node(@NonNull final String trusted_addr) {
            final int index_port = trusted_addr.indexOf(":");
            if (index_port != -1) {
                addr = trusted_addr.substring(0, index_port);
                port = Integer.parseInt(trusted_addr.substring(index_port + 1));
            } else {
                addr = trusted_addr;
                port = Network.NETWORK.getPort();
            }
        }

        public String toString(){
            return String.format("%s:%d", addr, port);
        }
    }
}
