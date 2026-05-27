import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { movieService } from '../../services/movieService';

const PreferenceGenres = ({ navigation }) => {
  const [genres, setGenres] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const loadGenres = useCallback(
    async (showLoader = false) => {
      try {
        if (showLoader) {
          setLoading(true);
        } else {
          setRefreshing(true);
        }
        setErrorMessage('');

        const response = await movieService.getRecommendationGenres();
        const items = Array.isArray(response?.genres) ? response.genres : [];
        setGenres(items);
      } catch (error) {
        setErrorMessage(error?.message || 'Unable to load genres.');
      } finally {
        if (showLoader) {
          setLoading(false);
        } else {
          setRefreshing(false);
        }
      }
    },
    []
  );

  useEffect(() => {
    loadGenres(true);
  }, [loadGenres]);

  const toggleGenre = (genreId) => {
    setSelectedIds((prev) =>
      prev.includes(genreId) ? prev.filter((id) => id !== genreId) : [...prev, genreId]
    );
  };

  const handleClear = () => {
    setSelectedIds([]);
  };

  const handleSave = async () => {
    if (!selectedIds.length) {
      Alert.alert('Notice', 'Please select at least one genre.');
      return;
    }

    try {
      setSaving(true);
      await movieService.selectRecommendationGenres(selectedIds);
      Alert.alert('Success', 'Your genre preferences have been saved.');
      navigation.goBack();
    } catch (error) {
      Alert.alert('Error', error?.message || 'Unable to save preferences.');
    } finally {
      setSaving(false);
    }
  };

  const selectedCountLabel = useMemo(() => {
    if (!selectedIds.length) return 'No genres selected';
    if (selectedIds.length === 1) return '1 genre selected';
    return `${selectedIds.length} genres selected`;
  }, [selectedIds]);

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Preferences</Text>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.skipText}>Skip</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.loadingWrap}>
          <ActivityIndicator size="large" color="#FF1E1E" />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={styles.content}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => loadGenres(false)}
              tintColor="#FF1E1E"
              colors={['#FF1E1E']}
              progressBackgroundColor="#1A0204"
            />
          }
        >
          <Text style={styles.title}>Pick your favorite
genres</Text>
          <Text style={styles.subtitle}>
            Select the types of movies you enjoy to get personalized recommendations.
          </Text>

          {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}

          <Text style={styles.selectionNote}>{selectedCountLabel}</Text>

          <View style={styles.genreGrid}>
            {genres.map((genre) => {
              const active = selectedIds.includes(genre.id);
              return (
                <TouchableOpacity
                  key={genre.id}
                  onPress={() => toggleGenre(genre.id)}
                  style={[styles.genreChip, active && styles.genreChipActive]}
                >
                  <Text style={[styles.genreText, active && styles.genreTextActive]}>{genre.name}</Text>
                </TouchableOpacity>
              );
            })}
          </View>

          <TouchableOpacity
            style={[styles.saveButton, (!selectedIds.length || saving) && styles.saveButtonDisabled]}
            onPress={handleSave}
            disabled={!selectedIds.length || saving}
          >
            <Text style={styles.saveButtonText}>
              {saving ? 'Saving...' : 'Save and Continue ->'}
            </Text>
          </TouchableOpacity>
        </ScrollView>
      )}
    </View>
  );
};

export default PreferenceGenres;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  header: {
    paddingTop: 50,
    paddingHorizontal: 18,
    paddingBottom: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headerTitle: {
    color: '#111827',
    fontSize: 16,
    fontWeight: '700',
  },
  skipText: {
    color: '#E53935',
    fontSize: 14,
    fontWeight: '700',
  },
  loadingWrap: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    padding: 18,
    paddingBottom: 80,
  },
  title: {
    color: '#111827',
    fontSize: 26,
    fontWeight: '800',
    lineHeight: 32,
  },
  subtitle: {
    marginTop: 6,
    color: '#6B7280',
    fontSize: 14,
    lineHeight: 20,
  },
  errorText: {
    marginTop: 12,
    color: '#E53935',
    fontSize: 14,
  },
  selectionNote: {
    marginTop: 16,
    color: '#6B7280',
    fontSize: 13,
    fontWeight: '600',
  },
  genreGrid: {
    marginTop: 18,
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  genreChip: {
    width: '48%',
    borderWidth: 1,
    borderColor: '#E5E7EB',
    borderRadius: 14,
    paddingVertical: 14,
    marginBottom: 12,
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  genreChipActive: {
    borderColor: '#E53935',
    backgroundColor: '#FDECEC',
  },
  genreText: {
    color: '#111827',
    fontSize: 15,
    fontWeight: '600',
  },
  genreTextActive: {
    color: '#E53935',
    fontWeight: '700',
  },
  saveButton: {
    marginTop: 24,
    backgroundColor: '#E53935',
    paddingVertical: 16,
    borderRadius: 28,
    alignItems: 'center',
  },
  saveButtonDisabled: {
    backgroundColor: '#F2B4B4',
  },
  saveButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
});
