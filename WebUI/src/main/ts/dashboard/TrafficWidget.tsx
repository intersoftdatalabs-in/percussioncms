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
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { subDays, format } from 'date-fns';
import { post } from '../api/client';
import { styles } from './dashboard.styles';

interface TrafficDataPoint {
  date?: string;
  time?: string;
  views?: number;
  visitors?: number;
  pageViews?: number;
  uniqueVisitors?: number;
  bounceRate?: number;
  [key: string]: unknown;
}

interface TrafficResponse {
  data?: TrafficDataPoint[];
  traffic?: TrafficDataPoint[];
  dataPoints?: TrafficDataPoint[];
  totalViews?: number;
  totalVisitors?: number;
  [key: string]: unknown;
}

export interface TrafficWidgetProps {
  title?: string;
  refreshInterval?: number;
  daysRange?: number;
  granularity?: 'daily' | 'hourly';
  chartType?: 'line' | 'bar';
}

/**
 * TrafficWidget displays content traffic analytics with an interactive chart.
 * Shows views, unique visitors, and trends over a configurable time period.
 * Uses Recharts for visualization with daily or hourly granularity options.
 */
export const TrafficWidget: React.FC<TrafficWidgetProps> = ({
  title = 'Content Traffic',
  refreshInterval = 300000, // 5 minutes
  daysRange = 30,
  granularity = 'daily',
  chartType = 'line',
}) => {
  const [data, setData] = useState<TrafficDataPoint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalViews, setTotalViews] = useState<number>(0);
  const [totalVisitors, setTotalVisitors] = useState<number>(0);

  useEffect(() => {
    const fetchTraffic = async () => {
      try {
        setIsLoading(true);
        setError(null);

        const startDate = subDays(new Date(), daysRange);
        const endDate = new Date();

        const payload = {
          startDate: format(startDate, 'yyyy-MM-dd'),
          endDate: format(endDate, 'yyyy-MM-dd'),
          granularity: granularity,
        };

        // Fetch traffic data
        const response = await post<TrafficResponse>('/services/activity/contenttraffic', payload);

        // Handle response format
        let dataArray: TrafficDataPoint[] = [];
        let views = 0;
        let visitors = 0;

        if (response.data && Array.isArray(response.data)) {
          dataArray = response.data;
        } else if (response.traffic && Array.isArray(response.traffic)) {
          dataArray = response.traffic;
        } else if (response.dataPoints && Array.isArray(response.dataPoints)) {
          dataArray = response.dataPoints;
        } else if (Array.isArray(response)) {
          dataArray = response as TrafficDataPoint[];
        }

        // Extract totals
        if (response.totalViews !== undefined) {
          views = response.totalViews;
        } else {
          views = dataArray.reduce((sum, point) => sum + ((point.views ?? 0) + (point.pageViews ?? 0)), 0);
        }

        if (response.totalVisitors !== undefined) {
          visitors = response.totalVisitors;
        } else {
          visitors = dataArray.reduce((sum, point) => sum + ((point.visitors ?? 0) + (point.uniqueVisitors ?? 0)), 0);
        }

        setData(dataArray);
        setTotalViews(views);
        setTotalVisitors(visitors);
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Failed to load traffic data';
        setError(errorMessage);
        console.error('TrafficWidget error:', err);
      } finally {
        setIsLoading(false);
      }
    };

    // Fetch immediately
    fetchTraffic();

    // Set up refresh interval
    const interval =
      refreshInterval > 0 ? setInterval(fetchTraffic, refreshInterval) : undefined;

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [refreshInterval, daysRange, granularity]);

  const renderChart = () => {
    if (!data || data.length === 0) {
      return (
        <div style={{ padding: '20px', textAlign: 'center', color: '#999' }}>
          No traffic data available
        </div>
      );
    }

    const chartData = data.map((point) => ({
      ...point,
      name: point.date || point.time || 'Data',
      views: point.views ?? point.pageViews ?? 0,
      visitors: point.visitors ?? point.uniqueVisitors ?? 0,
    }));

    return (
      <ResponsiveContainer width="100%" height={300}>
        {chartType === 'bar' ? (
          <BarChart data={chartData} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" fontSize={12} />
            <YAxis fontSize={12} />
            <Tooltip />
            <Legend />
            <Bar dataKey="views" fill="#8884d8" name="Page Views" />
            <Bar dataKey="visitors" fill="#82ca9d" name="Unique Visitors" />
          </BarChart>
        ) : (
          <LineChart data={chartData} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" fontSize={12} />
            <YAxis fontSize={12} />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="views" stroke="#8884d8" name="Page Views" />
            <Line type="monotone" dataKey="visitors" stroke="#82ca9d" name="Unique Visitors" />
          </LineChart>
        )}
      </ResponsiveContainer>
    );
  };

  const renderMetrics = () => {
    return (
      <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' } as React.CSSProperties}>
        <div style={{ flex: 1, padding: '12px', backgroundColor: '#e3f2fd', borderRadius: '4px' } as React.CSSProperties}>
          <div style={{ fontSize: '0.8em', color: '#666', marginBottom: '4px' }}>
            Page Views
          </div>
          <div style={{ fontSize: '1.5em', fontWeight: '700', color: '#1976d2' }}>
            {totalViews.toLocaleString()}
          </div>
        </div>
        <div style={{ flex: 1, padding: '12px', backgroundColor: '#e8f5e9', borderRadius: '4px' } as React.CSSProperties}>
          <div style={{ fontSize: '0.8em', color: '#666', marginBottom: '4px' }}>
            Unique Visitors
          </div>
          <div style={{ fontSize: '1.5em', fontWeight: '700', color: '#388e3c' }}>
            {totalVisitors.toLocaleString()}
          </div>
        </div>
      </div>
    );
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div style={styles.widgetLoading}>
          <p>Loading traffic data...</p>
        </div>
      );
    }

    if (error) {
      return (
        <div style={styles.widgetError}>
          <p>Error: {error}</p>
        </div>
      );
    }

    return (
      <div style={styles.widgetContent}>
        {renderMetrics()}
        {renderChart()}
      </div>
    );
  };

  return (
    <div style={styles.widget}>
      <div style={styles.widgetTitle}>{title}</div>
      {renderContent()}
    </div>
  );
};

export default TrafficWidget;
