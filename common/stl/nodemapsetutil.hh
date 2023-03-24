//
// Created by Dimitar Misev
// Copyright (c) 2017 rasdaman GmbH. All rights reserved.
//

#ifndef _COMMON_NODEMAPSETUTIL_HH_
#define _COMMON_NODEMAPSETUTIL_HH_

#include <functional>
#include <unordered_set>
#include <unordered_map>
#include <memory>

namespace common {

template <typename T>
using KeyHash = std::function<size_t(std::shared_ptr<T>)>;

template <typename T>
using KeyEqual = std::function<bool(std::shared_ptr<T>, std::shared_ptr<T>)>;

template <typename K, typename V>
using NodeMap =
    std::unordered_map<std::shared_ptr<K>, V, KeyHash<K>, KeyEqual<K>>;

template <typename K>
using NodeSet =
    std::unordered_set<std::shared_ptr<K>, KeyHash<K>, KeyEqual<K>>;

class NodeMapSetUtil {

 public:

  template <typename K>
  static KeyHash<K> getHasher() {
    static auto ret = [](std::shared_ptr<K> n) noexcept -> size_t {
      return std::hash<intptr_t>()(reinterpret_cast<intptr_t>(n.get()));
    };
    return ret;
  }
  template <typename K>
  static KeyEqual<K> getEqual() {
    static auto ret = [](std::shared_ptr<K> a, std::shared_ptr<K> b) noexcept -> bool {
      return a.get() == b.get();
    };
    return ret;
  }

  /**
   * @return a map of shared_ptr's of type K -> V with bare pointer hash/equality.
   */
  template <typename K, typename V>
  static NodeMap<K, V> makePointerNodeMap() {
    return NodeMap<K, V>{5, getHasher<K>(), getEqual<K>()};
  }

  /**
   * @return a set of shared_ptr's of type K with bare pointer hash/equality.
   */
  template <typename K>
  static NodeSet<K> makePointerNodeSet() {
    return NodeSet<K>{5, getHasher<K>(), getEqual<K>()};
  }
};

}

#endif

