/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useEffect, useState } from 'react';
import { get } from '../api/client';
import { styles } from './dashboard.styles';

interface Member {
  id: string;
  name: string;
  email: string;
  role?: string;
  status?: string;
  joinDate?: string;
}

interface Membership {
  totalMembers: number;
  activeMembers?: number;
  members?: Member[];
  lastUpdated?: string;
}

interface MembershipData {
  membership?: Membership;
  data?: Membership;
  members?: Membership;
  [key: string]: unknown;
}

export interface MembershipWidgetProps {
  title?: string;
  refreshInterval?: number;
}

/**
 * MembershipWidget displays user membership information and statistics.
 * Shows member counts, member list, roles, and status information.
 */
export const MembershipWidget: React.FC<MembershipWidgetProps> = ({
  title = 'Membership',
  refreshInterval,
}) => {
  const [membership, setMembership] = useState<Membership | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMembership = async () => {
      setIsLoading(true);
      try {
        const response = await get<MembershipData>('/services/membership/list');
        let membershipData: Membership | null = null;

        if (response.membership) {
          membershipData = response.membership;
        } else if (response.data) {
          membershipData = response.data;
        } else if (response.members) {
          membershipData = response.members;
        } else if (typeof response === 'object' && 'totalMembers' in response) {
          membershipData = {
            totalMembers: (response as Record<string, unknown>).totalMembers as number,
            activeMembers: (response as Record<string, unknown>).activeMembers as number | undefined,
            members: (response as Record<string, unknown>).members as Member[] | undefined,
            lastUpdated: (response as Record<string, unknown>).lastUpdated as string | undefined,
          };
        }

        setMembership(membershipData);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load membership data';
        setError(errorMessage);
      } finally {
        setIsLoading(false);
      }
    };

    fetchMembership();
    if (refreshInterval) {
      const interval = setInterval(fetchMembership, refreshInterval * 1000);
      return () => clearInterval(interval);
    }
  }, [refreshInterval]);

  if (isLoading) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetLoading}>Loading membership data...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetError}>{error}</div>
      </div>
    );
  }

  if (!membership) {
    return (
      <div style={styles.widget}>
        <div style={styles.widgetTitle}>{title}</div>
        <div style={styles.widgetContent}>No membership data available</div>
      </div>
    );
  }

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      <div style={styles.widgetContent}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' } as React.CSSProperties}>
          {/* Member Statistics */}
          <div style={{ display: 'flex', gap: '12px', justifyContent: 'space-between' } as React.CSSProperties}>
            <div style={{ flex: 1, padding: '8px', backgroundColor: '#f0f8f0', borderRadius: '3px' }}>
              <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>Total Members</div>
              <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#333' }}>
                {membership.totalMembers}
              </div>
            </div>
            {membership.activeMembers !== undefined && (
              <div style={{ flex: 1, padding: '8px', backgroundColor: '#f0f8ff', borderRadius: '3px' }}>
                <div style={{ fontSize: '0.75em', color: '#666', marginBottom: '2px' }}>Active</div>
                <div style={{ fontSize: '1.2em', fontWeight: '600', color: '#007ea8' }}>
                  {membership.activeMembers}
                </div>
              </div>
            )}
          </div>

          {/* Members List */}
          {membership.members && membership.members.length > 0 && (
            <div style={{ borderTop: '1px solid #eee', paddingTop: '8px' }}>
              <div style={{ fontSize: '0.85em', fontWeight: '600', marginBottom: '6px', color: '#333' }}>
                Recently Joined
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' } as React.CSSProperties}>
                {membership.members.slice(0, 5).map((member) => (
                  <div key={member.id} style={{ fontSize: '0.75em', padding: '4px', backgroundColor: '#f9f9f9', borderRadius: '2px' }}>
                    <div style={{ fontWeight: '600', color: '#333' }}>{member.name}</div>
                    <div style={{ fontSize: '0.9em', color: '#666' }}>{member.email}</div>
                    {member.role && (
                      <div style={{ fontSize: '0.8em', color: '#999' }}>
                        Role: {member.role}
                      </div>
                    )}
                  </div>
                ))}
                {membership.members.length > 5 && (
                  <div style={{ fontSize: '0.75em', color: '#999', fontStyle: 'italic', textAlign: 'center' }}>
                    +{membership.members.length - 5} more members
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Last Updated */}
          {membership.lastUpdated && (
            <div style={{ fontSize: '0.7em', color: '#999', marginTop: '4px', paddingTop: '8px', borderTop: '1px solid #eee' }}>
              Last updated: {membership.lastUpdated}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
