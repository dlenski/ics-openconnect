/*
 * Copyright (c) 2019.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package app.openconnect;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.Toast;
import app.openconnect.api.ExternalAppDatabase;
import app.openconnect.api.GrantPermissionsActivity;
import app.openconnect.core.OpenConnectManagementThread;
import app.openconnect.core.OpenVpnService;
import app.openconnect.core.ProfileManager;
import app.openconnect.core.VPNConnector;

/**
 * @author Terry E-mail: yaoxinghuo at 126 dot com
 * @date 2019-06-10 08:23
 * @description
 */
public class RemoteControlActivity extends Activity {

    public static final String EXTRA_NAME = "app.openconnect.api.profileName";

    private ExternalAppDatabase mExtAppDb;
    private VPNConnector mConn;

    @Override
    protected void onResume() {
        super.onResume();

        mExtAppDb = new ExternalAppDatabase(this);

        mConn = new VPNConnector(this, true) {
            @Override
            public void onUpdate(OpenVpnService service) {
                performAction(service);
            }
        };
    }

    private void performAction(OpenVpnService service) {

        if (!mExtAppDb.checkRemoteActionPermission(this, getCallingPackage())) {
            finish();
            return;
        }

        Intent intent = getIntent();
        if (intent == null) { // avoid crash

            finish();
            return;
        }
        setIntent(null);
        ComponentName component = intent.getComponent();
        if (component.getShortClassName().equals(".api.DisconnectVPN")) {
            service.stopVPN();
        } else if (component.getShortClassName().equals(".api.ConnectVPN")) {
            String vpnName = intent.getStringExtra(EXTRA_NAME);
            VpnProfile profile = ProfileManager.getProfileByName(vpnName);
            if (profile == null) {
                Toast.makeText(this, String.format("Vpn profile %s from API call not found", vpnName),
                        Toast.LENGTH_LONG).show();
            } else {
                if (service.getConnectionState() != OpenConnectManagementThread.STATE_DISCONNECTED) {
                    mConn.service.stopVPN();
                }
                startVPN(profile);
            }
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mConn.stopActiveDialog();
        mConn.unbind();
    }

    private void startVPN(VpnProfile profile) {
        Intent intent = new Intent(this, GrantPermissionsActivity.class);
        String pkg = this.getPackageName();

        intent.putExtra(pkg + GrantPermissionsActivity.EXTRA_UUID, profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }
}
