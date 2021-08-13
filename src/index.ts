import { registerPlugin } from '@capacitor/core';

import type { P2pConnectPlugin } from './definitions';

const P2pConnect = registerPlugin<P2pConnectPlugin>('P2pConnect', {
  web: () => import('./web').then(m => new m.P2pConnectWeb()),
});

export * from './definitions';
export { P2pConnect };
