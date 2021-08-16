import { registerPlugin } from '@capacitor/core';

import type { P2pConnectPlugin } from './definitions';

const P2pConnect = registerPlugin<P2pConnectPlugin>(
  'P2pConnect', 
  {}
  );

export * from './definitions';
export { P2pConnect };
