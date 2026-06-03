import { useState, useEffect, useCallback } from 'react';
import { fetchTranslations, createMessageFunction, Translations } from './api';

export interface UseTranslationsResult {
  translations: Record<string, string> | null;
  message: (key: string, args?: string[]) => string;
  locale: string;
  isLoading: boolean;
  error: Error | null;
}

export interface UseTranslationsOptions {
  locale?: string;
  prefix?: string;
  prefixes?: string[];
  lazy?: boolean;
}

export function useTranslations(options: UseTranslationsOptions = {}): UseTranslationsResult {
  const { locale = 'en-us', prefix, prefixes, lazy = false } = options;
  
  const [translations, setTranslations] = useState<Record<string, string> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(!lazy);
  const [error, setError] = useState<Error | null>(null);
  
  const loadTranslations = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const data: Translations = await fetchTranslations({ locale, prefix, prefixes });
      setTranslations(data.translations);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Failed to load translations'));
    } finally {
      setIsLoading(false);
    }
  }, [locale, prefix, prefixes]);
  
  useEffect(() => {
    if (!lazy) {
      loadTranslations();
    }
  }, [lazy, loadTranslations]);
  
  const message = useCallback((key: string, args?: string[]): string => {
    if (!translations) {
      const atSignIndex = key.indexOf('@');
      if (atSignIndex >= 0 && key.length > atSignIndex + 1) {
        return key.substring(atSignIndex + 1);
      }
      return key;
    }
    
    return createMessageFunction(translations)(key, args);
  }, [translations]);
  
  return {
    translations,
    message,
    locale,
    isLoading,
    error,
  };
}
