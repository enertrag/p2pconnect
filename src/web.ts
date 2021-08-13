import { WebPlugin } from '@capacitor/core';

import type { P2pConnectPlugin } from './definitions';

export class P2pConnectWeb extends WebPlugin implements P2pConnectPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
