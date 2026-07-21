# Quickstart validation: Linux systemd services (988)

## Prerequisites
- Linux host with systemd (or a packaging-only machine for structural tests)
- Built/installed CMS tree including `jetty/service/`
- Root for live install tests; **no root** needed for Maven structural tests

## Structural (CI / dev workstation)

```bash
./mvn-env.sh -pl modules/perc-jetty test -Dai.integrity.skip=true
```

Expect tests covering unit template contract keys (see `contracts/systemd-unit-contract.md`).

## Live install smoke (manual)

```bash
# From CMS install, as root (layout: <rxDir>/jetty/service after distribution extract)
cd <rxDir>/jetty/service
./install-jetty-service.sh PercussionCMS install
# optional: --systemd (require) | --initd (force SysV)

systemctl daemon-reload   # already run by installer; safe to repeat
systemctl status PercussionCMS
systemctl start PercussionCMS
systemctl is-active PercussionCMS   # expect: active
journalctl -u PercussionCMS -n 50 --no-pager

systemctl stop PercussionCMS
systemctl is-active PercussionCMS   # expect: inactive

./install-jetty-service.sh PercussionCMS uninstall
```

## Slow-start check (manual)

Simulate long start (or run post-upgrade) and confirm:
- No fail at ~90s solely due to timeout if start finishes within 30 minutes
- Journal or start.log shows progress / pointer to logs

## DTS Production / Staging (manual)

```bash
cd <dtsInstallRoot>   # directory with DTSProductionService.sh and bin/catalina.sh
sudo ./DTSProductionService.sh install
sudo systemctl start PercussionProductionDTS
journalctl -u PercussionProductionDTS -n 50 --no-pager

sudo ./DTSStagingService.sh install
sudo systemctl start PercussionStagingDTS
```

## Init.d fallback (manual)

```bash
./install-jetty-service.sh PercussionCMS install --initd
./DTSProductionService.sh install --initd
# or host without systemd
```

## Expected outcomes
- SC-001–SC-004 satisfied on smoke host  
- SC-005 satisfied in CI via structural tests  
