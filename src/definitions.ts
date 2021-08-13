export interface P2pConnectPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
