import { ChannelCredentials, createChannel, createClient } from "nice-grpc";

import {
  LeetcodeAuthServiceDefinition,
  type GetTokenResponse,
  type LeetcodeAuthServiceClient,
  type RefreshTokenResponse,
} from "@/generated/leetcode_auth";

export class LasClient implements AsyncDisposable {
  private readonly channel;
  private readonly client: LeetcodeAuthServiceClient;

  constructor(private readonly host: string) {
    this.channel = createChannel(host, ChannelCredentials.createSsl());
    this.client = createClient(LeetcodeAuthServiceDefinition, this.channel);
  }

  async getToken(): Promise<GetTokenResponse> {
    const response = await this.client.getToken(
      {},
      { signal: AbortSignal.timeout(10_000) },
    );

    if (!response.token || !response.csrf) {
      throw new Error(`LAS at ${this.host} returned no LeetCode credentials.`);
    }

    if (!response.username) {
      throw new Error(
        `LAS at ${this.host} returned no username. Deploy a leetcode-auth-service build whose getToken returns username.`,
      );
    }

    return response;
  }

  async refreshToken(): Promise<RefreshTokenResponse> {
    return this.client.refreshToken(
      {},
      { signal: AbortSignal.timeout(10_000) },
    );
  }

  async [Symbol.asyncDispose](): Promise<void> {
    this.channel.close();
  }
}
