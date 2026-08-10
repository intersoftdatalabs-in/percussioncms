# DTS logrotate sample (standalone package)

**Issue:** [#2348](https://github.com/intersoftdatalabs-in/percussioncms/issues/2348)

Standalone DTS ships `logrotate/percussion-dts` at the install root for operators who run DTS on a host without a full CMS tree.

|       File       |                                                Role                                                 |
|------------------|-----------------------------------------------------------------------------------------------------|
| `percussion-dts` | Linux `logrotate` fragment for `Deployment/Server/logs` (`*.log`, `*.out` including `catalina.out`) |
| `README.md`      | This note                                                                                           |

**Not auto-enabled.** Copy to `/etc/logrotate.d/` only with operator consent after substituting the install root and running `logrotate -d`.

```bash
INSTALL_ROOT=/opt/Percussion   # this DTS install root
sed "s|/opt/Percussion|${INSTALL_ROOT}|g" \
  "${INSTALL_ROOT}/logrotate/percussion-dts" \
  | sudo tee /etc/logrotate.d/percussion-dts >/dev/null
sudo chmod 0644 /etc/logrotate.d/percussion-dts
sudo logrotate -d /etc/logrotate.d/percussion-dts
```

Defaults: daily, rotate 14, compress, delaycompress, **copytruncate**, missingok, notifempty.

Full operator guide (CMS + DTS coexistence with Log4j2 and `perc-doctor clean-logs`, Windows Task Scheduler sample) lives with the CMS distribution at:

`rxconfig/Installer/logrotate/README.md`

Related: `README-systemd.md` (Linux service dual-ship), `README-windows-service.md`.
